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
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;

public class PageCouchbaseService {
    private final String couchBaseURI;
    private final String username;
    private final String password;
    private final String bucketName;

    public PageCouchbaseService(String couchBaseURI, String username, String password, String bucketName) {
        this.couchBaseURI = couchBaseURI;
        this.username = username;
        this.password = password;
        this.bucketName = bucketName;
    }

    private Cluster connectToCluster() {
        return Cluster.connect(couchBaseURI, username, password);
    }

    private JsonObject getDocumentById(Collection collection, String id) {
        try {
            GetResult getResult = collection.get(id);
            return getResult.contentAsObject();
        } catch (DocumentNotFoundException e) {
            throw new NotFoundResponse("Page not found");
        } catch (CouchbaseException e) {
            throw new InternalServerErrorResponse("Error al acceder a la base de datos");
        }
    }

    public String findByID(String id) {
        try (Cluster cluster = connectToCluster()) {
            Bucket bucket = cluster.bucket(bucketName);
            Scope scope = bucket.defaultScope();
            Collection postsCollection = scope.collection("pages");

            JsonObject document = getDocumentById(postsCollection, id);

            return JsonArray.create().add(document).toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

}
