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
      NSDictionary *args = call.arguments;
      
      NSString *path = [args objectForKey:@"path"];
      NSString *target = [args objectForKey:@"target"];
      NSNumber *width = [args objectForKey:@"width"];
      NSNumber *height = [args objectForKey:@"height"];
      
      if ([[NSFileManager defaultManager] fileExistsAtPath: target]) {
          result([FlutterError errorWithCode:@"MediaInfo" message:@"FileOverwriteDenied" details:nil]);
          return;
      }
      
      NSURL *mediaURL = [NSURL fileURLWithPath:path];
      AVAsset *asset = [AVURLAsset URLAssetWithURL:mediaURL options:nil];

      CGFloat durationSeconds = CMTimeGetSeconds(asset.duration);
      AVAssetImageGenerator *generator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
      
      generator.appliesPreferredTrackTransform = YES;
      
      CMTime time = CMTimeMakeWithSeconds(durationSeconds / 3.0, 600);
      
      [generator generateCGImagesAsynchronouslyForTimes:@[[NSValue valueWithCMTime:time]]
                                      completionHandler:^(CMTime requestedTime,
                                                          CGImageRef  _Nullable image,
                                                          CMTime actualTime,
                                                          AVAssetImageGeneratorResult generatorResult,
                                                          NSError * _Nullable error) {
                                          if (error) {
                                              NSLog(@"Can not generate image: %@", error);
                                              result([FlutterError errorWithCode:@"MediaInfo" message:@"FileCreationFailed" details:nil]);
                                          }
                                          
                                          UIGraphicsBeginImageContext(CGSizeMake(width.intValue, height.intValue));
                                          UIImage *img = [UIImage imageWithCGImage:image];
                                          [img drawInRect:CGRectMake(0, 0, width.intValue, height.intValue)];
                                          UIImage *resized = UIGraphicsGetImageFromCurrentImageContext();
                                          UIGraphicsEndImageContext();
                                          
                                          NSData *jpgData = UIImageJPEGRepresentation(resized, 80);
                                          
                                          [jpgData writeToFile:target atomically:YES];
                                          
                                          result(target);
                                      }];
      
  } else {
    result(FlutterMethodNotImplemented);
  }
}

@end
