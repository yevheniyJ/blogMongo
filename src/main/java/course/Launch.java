package course;


import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import course.configuration.FreemarkerBasedRoute;
import course.controller.BlogPostController;
import course.controller.UserController;
import course.dao.BlogPostDAO;
import course.dao.SessionDAO;
import course.dao.UserDAO;
import freemarker.template.Configuration;

import java.io.IOException;

import static spark.Spark.setPort;

public class Launch {

  private final Configuration cfg;

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      new Launch("mongodb://localhost");
    } else {
      new Launch(args[0]);
    }
  }

  public Launch(String mongoURIString) throws IOException {
    final MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoURIString));
    final MongoDatabase blogDatabase = mongoClient.getDatabase("blog");

    cfg = FreemarkerBasedRoute.createFreemarkerConfiguration();

    setPort(8082);

    BlogPostDAO blogPostDAO = new BlogPostDAO(blogDatabase);
    SessionDAO sessionDAO = new SessionDAO(blogDatabase);
    UserDAO userDAO = new UserDAO(blogDatabase);

    new BlogPostController(blogPostDAO, cfg, sessionDAO);
    new UserController(userDAO, cfg, sessionDAO);
  }

}
