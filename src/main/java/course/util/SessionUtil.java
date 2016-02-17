package course.util;

import spark.Request;

import javax.servlet.http.Cookie;

public class SessionUtil {

  private SessionUtil(){
    throw new UnsupportedOperationException();
  }

  // helper function to get session cookie as string
  public static String getSessionCookie(final Request request) {
    if (request.raw().getCookies() == null) {
      return null;
    }
    for (Cookie cookie : request.raw().getCookies()) {
      if (cookie.getName().equals("session")) {
        return cookie.getValue();
      }
    }
    return null;
  }

  // helper function to get session cookie as string
  public static Cookie getSessionCookieActual(final Request request) {
    if (request.raw().getCookies() == null) {
      return null;
    }
    for (Cookie cookie : request.raw().getCookies()) {
      if (cookie.getName().equals("session")) {
        return cookie;
      }
    }
    return null;
  }
}
