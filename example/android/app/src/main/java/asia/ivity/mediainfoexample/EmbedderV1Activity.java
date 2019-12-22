package asia.ivity.mediainfoexample;

import android.os.Bundle;

import asia.ivity.mediainfo.MediaInfoPlugin;
import io.flutter.app.FlutterActivity;

public class EmbedderV1Activity extends FlutterActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MediaInfoPlugin.registerWith(registrarFor("asia.ivity.mediainfo.MediaInfoPlugin"));
  }
}
