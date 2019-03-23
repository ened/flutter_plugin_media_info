#import "MediaInfoPlugin.h"

#import <AVFoundation/AVFoundation.h>

@implementation MediaInfoPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"asia.ivity.flutter/media_info"
            binaryMessenger:[registrar messenger]];
  MediaInfoPlugin* instance = [[MediaInfoPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
    
    NSURL *u= [[[NSFileManager defaultManager] URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask] firstObject];
    
    NSLog(@"documents: %@", u);
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getMediaInfo" isEqualToString:call.method]) {
      NSMutableDictionary *d = [NSMutableDictionary dictionary];
      
      NSURL *mediaURL = [NSURL fileURLWithPath:call.arguments]; // Your video's URL
      AVURLAsset *asset = [AVURLAsset URLAssetWithURL:mediaURL options:nil];
      NSArray *tracks = [asset tracksWithMediaType:AVMediaTypeVideo];
      AVAssetTrack *track = [tracks objectAtIndex:0];
      
      [d setValue:[NSNumber numberWithInteger:(NSInteger) track.naturalSize.width]
           forKey:@"width"];
      [d setValue:[NSNumber numberWithInteger:(NSInteger) track.naturalSize.height]
           forKey:@"height"];
      [d setValue:[NSNumber numberWithFloat:track.nominalFrameRate]
           forKey:@"frameRate"];
      [d setValue:[NSNumber numberWithLong:CMTimeGetSeconds(track.timeRange.duration) * 1000]
           forKey:@"durationMs"];
      [d setValue:[NSNumber numberWithInteger:[tracks count]]
           forKey:@"numTracks"];
      // TODO: Need to find a method to determine the mimetype.

      result(d);
  } else if ([@"generateThumbnail" isEqualToString:call.method]) {
      
      result([NSNumber numberWithBool:NO]);
  } else {
    result(FlutterMethodNotImplemented);
  }
}

@end
