package asia.ivity.mediainfo;

import java.util.HashMap;

public class AudioDetail implements MediaDetail {
  private final long durationMs;
  private final int bitrate;
  private final String mimeType;

  public AudioDetail(long durationMs, int bitrate, String mimeType) {
    this.durationMs = durationMs;
    this.bitrate = bitrate;
    this.mimeType = mimeType;
  }

  @Override
  public HashMap<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<>();

    map.put("durationMs", durationMs);
    map.put("mimeType", mimeType);
    map.put("bitrate", bitrate);

    return map;
  }
}
