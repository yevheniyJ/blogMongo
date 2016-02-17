package course.configuration;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public abstract class FreemarkerBasedRoute extends Route {

  private final Template template;

  protected FreemarkerBasedRoute(final String path, final String templateName, Configuration cfg)
      throws IOException {
    super(path);
    template = cfg.getTemplate(templateName);
  }

  @Override
  public Object handle(Request request, Response response) {
    StringWriter writer = new StringWriter();
    try {
      doHandle(request, response, writer);
    } catch (Exception e) {
      e.printStackTrace();
      response.redirect("/internal_error");
    }
    return writer;
  }

  protected abstract void doHandle(final Request request, final Response response,
      final Writer writer) throws IOException, TemplateException;

  public Template getTemplate() {
    return template;
  }

  public static Configuration createFreemarkerConfiguration() {
    Configuration retVal = new Configuration();
    retVal.setClassForTemplateLoading(FreemarkerBasedRoute.class, "/freemarker");
    return retVal;
  }
}
