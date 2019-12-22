import 'package:flutter_test/flutter_test.dart';
import 'package:media_info/media_info.dart';
import 'package:e2e/e2e.dart';

void main() {
  E2EWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Can get landscape details', (WidgetTester tester) async {
    final MediaInfo mediaInfo = MediaInfo();
    // final int batteryLevel = await battery.batteryLevel;
    // expect(batteryLevel, isNotNull);
  });
}