package course.controller;

import course.configuration.FreemarkerBasedRoute;
import course.dao.SessionDAO;
import course.dao.UserDAO;
import course.util.SessionUtil;
import course.util.ValidatorUtil;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.Document;
import spark.Request;
import spark.Response;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import static spark.Spark.get;
import static spark.Spark.post;

public class UserController  {

  private final UserDAO userDAO;
  private final SessionDAO sessionDAO;
  private final Configuration cfg;

  public UserController(UserDAO userDAO, Configuration cfg, SessionDAO sessionDAO)
      throws IOException {
    this.userDAO = userDAO;
    this.cfg = cfg;
    this.sessionDAO = sessionDAO;
    initializeRoutes();
  }

  private void initializeRoutes() throws IOException {
    // handle the signup post
    post(new FreemarkerBasedRoute("/signup", "signup.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {
        String email = request.queryParams("email");
        String username = request.queryParams("username");
        String password = request.queryParams("password");
        String verify = request.queryParams("verify");

        HashMap<String, String> root = new HashMap<>();
        root.put("username", StringEscapeUtils.escapeHtml4(username));
        root.put("email", StringEscapeUtils.escapeHtml4(email));

        if (ValidatorUtil.validateSignup(username, password, verify, email, root)) {
          // good user
          System.out.println("Signup: Creating user with: " + username + " " + password);
          if (!userDAO.addUser(username, password, email)) {
            // duplicate user
            root.put("username_error", "Username already in use, Please choose another");
            this.getTemplate().process(root, writer);
          } else {
            // good user, let's start a session
            String sessionID = sessionDAO.startSession(username);
            System.out.println("Session ID is" + sessionID);

            response.raw().addCookie(new Cookie("session", sessionID));
            response.redirect("/welcome");
          }
        } else {
          // bad signup
          System.out.println("User Registration did not validate");
          this.getTemplate().process(root, writer);
        }
      }
    });

    // present signup form for blog
    get(new FreemarkerBasedRoute("/signup", "signup.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        SimpleHash root = new SimpleHash();

        // initialize values for the form.
        root.put("username", "");
        root.put("password", "");
        root.put("email", "");
        root.put("password_error", "");
        root.put("username_error", "");
        root.put("email_error", "");
        root.put("verify_error", "");

        this.getTemplate().process(root, writer);
      }
    });

    get(new FreemarkerBasedRoute("/welcome", "welcome.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        String cookie = SessionUtil.getSessionCookie(request);
        String username = sessionDAO.findUserNameBySessionId(cookie);

        if (username == null) {
          System.out.println("welcome() can't identify the user, redirecting to signup");
          response.redirect("/signup");

        } else {
          SimpleHash root = new SimpleHash();

          root.put("username", username);

          this.getTemplate().process(root, writer);
        }
      }
    });

    // present the login page
    get(new FreemarkerBasedRoute("/login", "login.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {
        SimpleHash root = new SimpleHash();

        root.put("username", "");
        root.put("login_error", "");

        this.getTemplate().process(root, writer);
      }
    });

    // process output coming from login form. On success redirect folks to the welcome page
    // on failure, just return an error and let them try again.
    post(new FreemarkerBasedRoute("/login", "login.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        String username = request.queryParams("username");
        String password = request.queryParams("password");

        System.out.println("Login: User submitted: " + username + "  " + password);

        Document user = userDAO.validateLogin(username, password);

        if (user != null) {

          // valid user, let's log them in
          String sessionID = sessionDAO.startSession(user.get("_id").toString());

          if (sessionID == null) {
            response.redirect("/internal_error");
          } else {
            // set the cookie for the user's browser
            response.raw().addCookie(new Cookie("session", sessionID));

            response.redirect("/welcome");
          }
        } else {
          SimpleHash root = new SimpleHash();


          root.put("username", StringEscapeUtils.escapeHtml4(username));
          root.put("password", "");
          root.put("login_error", "Invalid Login");
          this.getTemplate().process(root, writer);
        }
      }
    });

    // allows the user to logout of the blog
    get(new FreemarkerBasedRoute("/logout", "signup.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        String sessionID = SessionUtil.getSessionCookie(request);

        if (sessionID == null) {
          // no session to end
          response.redirect("/login");
        } else {
          // deletes from session table
          sessionDAO.endSession(sessionID);

          // this should delete the cookie
          Cookie c = SessionUtil.getSessionCookieActual(request);
          c.setMaxAge(0);

          response.raw().addCookie(c);

          response.redirect("/login");
        }
      }
    });

    // used to process internal errors
    get(new FreemarkerBasedRoute("/internal_error", "error_template.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {
        SimpleHash root = new SimpleHash();

        root.put("error", "System has encountered an error.");
        this.getTemplate().process(root, writer);
      }
    });
  }
}
