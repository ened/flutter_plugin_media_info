package asia.ivity.mediainfo;

import java.util.HashMap;

public class VideoDetail {

  private final int width;
  private final int height;
  private final float frameRate;
  private final long durationMs;
  private final short numTracks;
  private final String mimeType;

  public VideoDetail(int width, int height, float frameRate, long durationMs, short numTracks,
      String mimeType) {
    this.width = width;
    this.height = height;
    this.frameRate = frameRate;
    this.durationMs = durationMs;
    this.numTracks = numTracks;
    this.mimeType = mimeType;
  }

  HashMap<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<>();

    map.put("width", width);
    map.put("height", height);
    map.put("frameRate", frameRate);
    map.put("durationMs", durationMs);
    map.put("numTracks", (int) numTracks);
    map.put("mimeType", mimeType);

    return map;
  }
}
