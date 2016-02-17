package course.util;

import java.util.ArrayList;

public class TagsUtil {

  private TagsUtil(){
    throw new UnsupportedOperationException();
  }

  // tags the tags string and put it into an array
  public static ArrayList<String> extractTags(String tags) {

    tags = tags.replaceAll("\\s", "");
    String tagArray[] = tags.split(",");

    // let's clean it up, removing the empty string and removing dups
    ArrayList<String> cleaned = new ArrayList<String>();
    for (String tag : tagArray) {
      if (!tag.equals("") && !cleaned.contains(tag)) {
        cleaned.add(tag);
      }
    }

    return cleaned;
  }
}
