package servicios;


import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.SearchResult;
import com.couchbase.client.java.search.result.SearchRow;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;

import java.util.List;

import static com.couchbase.client.java.query.QueryOptions.queryOptions;


public class PostCouchbaseService {

    private final String couchBaseURI;
    private final String username;
    private final String password;
    private final String bucketName;

    public PostCouchbaseService(String couchBaseURI, String username, String password, String bucketName) {
        this.couchBaseURI = couchBaseURI;
        this.username = username;
        this.password = password;
        this.bucketName = bucketName;
    }

    private Cluster connectToCluster() {
        return Cluster.connect(couchBaseURI, username, password);
    }

    public String findLatestPosts() {
        try (Cluster cluster = connectToCluster()) { // try-with-resources : cierra el cluster automáticamente
            Bucket bucket = cluster.bucket(bucketName);
            Scope scope = bucket.defaultScope();

            QueryResult result = scope.query("SELECT id, title, resume FROM posts ORDER BY date DESC LIMIT 4");

            return result.rowsAsObject().toString();

        } catch (CouchbaseException e) {
            throw new InternalServerErrorResponse("Error while accessing database");
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private JsonObject getDocumentById(Collection collection, String id) {
        try {
            GetResult getResult = collection.get(id);
            return getResult.contentAsObject();
        } catch (DocumentNotFoundException e) {
            throw new NotFoundResponse("Post not found");
        } catch (CouchbaseException e) {
            throw new InternalServerErrorResponse("Error al acceder a la base de datos");
        }
    }

    public String findByID(String id) {
        try (Cluster cluster = connectToCluster()) {
            Bucket bucket = cluster.bucket(bucketName);
            Scope scope = bucket.defaultScope();
            Collection postsCollection = scope.collection("posts");

            JsonObject document = getDocumentById(postsCollection, id);

            return JsonArray.create().add(document).toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String numberPostsByAuthor() {

        try (Cluster cluster = connectToCluster()) {
            Bucket bucket = cluster.bucket(bucketName);
            Scope scope = bucket.defaultScope();

            String query = "SELECT author AS id, COUNT(*) AS count FROM posts GROUP BY author ORDER BY count DESC";
            QueryResult queryResult = scope.query(query);

            return queryResult.rowsAsObject().toString();

        } catch (CouchbaseException e) {
            throw new InternalServerErrorResponse("Error while accessing database");
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String postsByAuthor(String author) {

        try (Cluster cluster = connectToCluster()) {
            Bucket bucket = cluster.bucket(bucketName);
            Scope scope = bucket.defaultScope();

            QueryResult result = scope.query("SELECT * FROM posts WHERE author = $author",
                    queryOptions().parameters(JsonObject.create().put("author", author)));

            List<JsonObject> results = result.rowsAsObject(); // Devuelve un array con objetos posts, en el que cada uno tiene los detalles de un posteo
            JsonArray jsonArray = JsonArray.create();

            for (JsonObject post : results) {
                // Accedo al objeto 'posts' dentro de cada objeto 'post'
                JsonObject postDetails = post.getObject("posts");

                // Extraemos los campos necesarios del objeto 'postDetails'
                JsonObject postInfo = JsonObject.create().
                        put("id", postDetails.get("id")).
                        put("title", postDetails.get("title")).
                        put("resume", postDetails.get("resume")).
                        put("text", postDetails.get("text")).
                        put("tags", postDetails.get("tags")).
                        put("relatedLinks", postDetails.get("relatedLinks")).
                        put("author", postDetails.get("author")).
                        put("date", postDetails.get("date"));

                jsonArray.add(postInfo);
            }

            return jsonArray.toString();

        } catch (CouchbaseException e) {
            throw new InternalServerErrorResponse("Error while accessing database");
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String search(String text) {

        try (Cluster cluster = connectToCluster()) {
            Bucket bucket = cluster.bucket(bucketName);
            Scope scope = bucket.defaultScope();
            Collection postsCollection = scope.collection("posts");

            // Procesamos la busqueda indexada
            SearchResult result = cluster.searchQuery("text_idx", SearchQuery.queryString(text));

            JsonArray jsonArray = JsonArray.create();

            for (SearchRow row : result.rows()) {
                JsonObject document = this.getDocumentById(postsCollection,row.id());

                // Extraemos los campos necesarios
                JsonObject postInfo = JsonObject.create().
                        put("id", document.get("id")).
                        put("title", document.get("title")).
                        put("resume", document.get("resume")).
                        put("author", document.get("author"));

                JsonObject dateObj = document.getObject("date");
                postInfo.put("date", dateObj.getString("$date"));

                jsonArray.add(postInfo);
            }

            return jsonArray.toString();

        } catch (CouchbaseException e) {
            throw new InternalServerErrorResponse("Error while accessing database");
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
