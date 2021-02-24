package asia.ivity.mediainfoexample;

import android.os.Bundle;

import asia.ivity.mediainfo.MediaInfoPlugin;
import dev.flutter.plugins.e2e.E2EPlugin;
import io.flutter.app.FlutterActivity;

public class EmbedderV1Activity extends FlutterActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MediaInfoPlugin.registerWith(registrarFor("asia.ivity.mediainfo.MediaInfoPlugin"));
    E2EPlugin.registerWith(registrarFor("dev.flutter.plugins.e2e.E2EPlugin"));
  }
}
