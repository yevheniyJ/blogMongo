package course.util;

import java.util.HashMap;

public class ValidatorUtil {

  private ValidatorUtil() {
    throw new UnsupportedOperationException();
  }

  public static boolean validateSignup(String username, String password, String verify,
      String email, HashMap<String, String> errors) {
    String USER_RE = "^[a-zA-Z0-9_-]{3,20}$";
    String PASS_RE = "^.{3,20}$";
    String EMAIL_RE = "^[\\S]+@[\\S]+\\.[\\S]+$";

    errors.put("username_error", "");
    errors.put("password_error", "");
    errors.put("verify_error", "");
    errors.put("email_error", "");

    if (!username.matches(USER_RE)) {
      errors.put("username_error", "invalid username. try just letters and numbers");
      return false;
    }

    if (!password.matches(PASS_RE)) {
      errors.put("password_error", "invalid password.");
      return false;
    }


    if (!password.equals(verify)) {
      errors.put("verify_error", "password must match");
      return false;
    }

    if (!email.equals("")) {
      if (!email.matches(EMAIL_RE)) {
        errors.put("email_error", "Invalid Email Address");
        return false;
      }
    }

    return true;
  }
}
