package main;

import servicios.PageCouchbaseService;
import servicios.PostCouchbaseService;
import web.WebAPI;

public class Main {

    public static void main(String[] args) {

        String couchBaseURI = "couchbase://127.0.0.1";
        String username = "Administrator";
        String password = "admin123";
        String bucketName = "blogDB";

        PageCouchbaseService pages = new PageCouchbaseService(couchBaseURI, username, password, bucketName);
        PostCouchbaseService posts = new PostCouchbaseService(couchBaseURI, username, password, bucketName);

        WebAPI webApi = new WebAPI(pages, posts, 4567);
        webApi.start();

    }


}




