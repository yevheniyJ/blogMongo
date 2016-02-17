package course.controller;

import course.configuration.FreemarkerBasedRoute;
import course.dao.BlogPostDAO;
import course.dao.SessionDAO;
import course.util.SessionUtil;
import course.util.TagsUtil;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.Document;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.post;

public class BlogPostController {

  private final BlogPostDAO blogPostDAO;
  private final SessionDAO sessionDAO;
  private final Configuration cfg;

  public BlogPostController(BlogPostDAO blogPostDAO, Configuration cfg, SessionDAO sessionDAO)
      throws IOException {
    this.blogPostDAO = blogPostDAO;
    this.cfg = cfg;
    this.sessionDAO = sessionDAO;
    initializeRoutes();
  }

  private void initializeRoutes() throws IOException {
    // this is the blog home page
    get(new FreemarkerBasedRoute("/", "blog_template.ftl", cfg) {
      @Override
      public void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        List<Document> posts = blogPostDAO.findByDateDescending(10);
        SimpleHash root = new SimpleHash();

        root.put("myposts", posts);
        setUsernameParameter(request, root);

        this.getTemplate().process(root, writer);
      }
    });

    // used to display actual blog post detail page
    get(new FreemarkerBasedRoute("/post/:permalink", "entry_template.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {
        String permalink = request.params(":permalink");

        System.out.println("/post: get " + permalink);

        Document post = blogPostDAO.findByPermalink(permalink);
        if (post == null) {
          response.redirect("/post_not_found");
        } else {
          // empty comment to hold new comment in form at bottom of blog entry detail page
          SimpleHash newComment = new SimpleHash();
          newComment.put("name", "");
          newComment.put("email", "");
          newComment.put("body", "");

          SimpleHash root = new SimpleHash();

          root.put("post", post);
          root.put("comment", newComment);
          setUsernameParameter(request, root);

          this.getTemplate().process(root, writer);
        }
      }
    });

    // will present the form used to process new blog posts
    get(new FreemarkerBasedRoute("/newpost", "newpost_template.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        SimpleHash root = new SimpleHash();
        if (setUsernameParameter(request, root)) {
          this.getTemplate().process(root, writer);
        } else {
          response.redirect("/login");
        }
      }
    });

    // handle the new post submission
    post(new FreemarkerBasedRoute("/newpost", "newpost_template.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        String title = StringEscapeUtils.escapeHtml4(request.queryParams("subject"));
        String post = StringEscapeUtils.escapeHtml4(request.queryParams("body"));
        String tags = StringEscapeUtils.escapeHtml4(request.queryParams("tags"));

        String username = sessionDAO.findUserNameBySessionId(SessionUtil.getSessionCookie(request));

        if (username == null) {
          response.redirect("/login");    // only logged in users can post to blog
        } else if (title.equals("") || post.equals("")) {
          // redisplay page with errors
          HashMap<String, String> root = new HashMap<>();
          root.put("errors", "post must contain a title and blog entry.");
          root.put("subject", title);
          root.put("username", username);
          root.put("tags", tags);
          root.put("body", post);
          this.getTemplate().process(root, writer);
        } else {
          // extract tags
          ArrayList<String> tagsArray = TagsUtil.extractTags(tags);

          // substitute some <p> for the paragraph breaks
          post = post.replaceAll("\\r?\\n", "<p>");

          String permalink = blogPostDAO.addPost(title, post, tagsArray, username);

          // now redirect to the blog permalink
          response.redirect("/post/" + permalink);
        }
      }
    });

    // process a new comment
    post(new FreemarkerBasedRoute("/newcomment", "entry_template.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {
        String name = StringEscapeUtils.escapeHtml4(request.queryParams("commentName"));
        String email = StringEscapeUtils.escapeHtml4(request.queryParams("commentEmail"));
        String body = StringEscapeUtils.escapeHtml4(request.queryParams("commentBody"));
        String permalink = request.queryParams("permalink");

        Document post = blogPostDAO.findByPermalink(permalink);
        if (post == null) {
          response.redirect("/post_not_found");
        }
        // check that comment is good
        else if (name.equals("") || body.equals("")) {
          // bounce this back to the user for correction
          SimpleHash root = new SimpleHash();
          SimpleHash comment = new SimpleHash();

          comment.put("name", name);
          comment.put("email", email);
          comment.put("body", body);
          root.put("comment", comment);
          root.put("post", post);
          root.put("errors", "Post must contain your name and an actual comment");

          this.getTemplate().process(root, writer);
        } else {
          blogPostDAO.addPostComment(name, email, body, permalink);
          response.redirect("/post/" + permalink);
        }
      }
    });


    // Show the posts filed under a certain tag
    get(new FreemarkerBasedRoute("/tag/:thetag", "blog_template.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        String username = sessionDAO.findUserNameBySessionId(SessionUtil.getSessionCookie(request));
        SimpleHash root = new SimpleHash();

        String tag = StringEscapeUtils.escapeHtml4(request.params(":thetag"));
        List<Document> posts = blogPostDAO.findByTagDateDescending(tag);

        root.put("myposts", posts);
        if (username != null) {
          root.put("username", username);
        }

        this.getTemplate().process(root, writer);
      }
    });

    // will allow a user to click Like on a post
    post(new FreemarkerBasedRoute("/like", "entry_template.ftl", cfg) {
      @Override
      protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {

        String permalink = request.queryParams("permalink");
        String commentOrdinalStr = request.queryParams("comment_ordinal");


        // look up the post in question

        int ordinal = Integer.parseInt(commentOrdinalStr);

        String username = sessionDAO.findUserNameBySessionId(SessionUtil.getSessionCookie(request));
        if(username == null){
          response.redirect("/");
          return;
        }
        Document post = blogPostDAO.findByPermalink(permalink);

        //  if post not found, redirect to post not found error
        if (post == null) {
          response.redirect("/post_not_found");
        } else {
          blogPostDAO.likePost(permalink, ordinal);

          response.redirect("/post/" + permalink);
        }
      }
    });

    // tells the user that the URL is dead
    get(new FreemarkerBasedRoute("/post_not_found", "post_not_found.ftl", cfg) {
      @Override protected void doHandle(Request request, Response response, Writer writer)
          throws IOException, TemplateException {
        SimpleHash root = new SimpleHash();
        this.getTemplate().process(root, writer);
      }
    });
  }

  private boolean setUsernameParameter(Request request, SimpleHash root){
    String username = sessionDAO.findUserNameBySessionId(SessionUtil.getSessionCookie(request));
    if (username != null) {
      root.put("username", username);
      return true;
    }else{
      return false;
    }
  }
}
