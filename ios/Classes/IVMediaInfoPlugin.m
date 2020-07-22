#import "IVMediaInfoPlugin.h"

#import <AVFoundation/AVFoundation.h>
#import <ImageIO/ImageIO.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <GLKit/GLKit.h>

@interface IVMediaInfoPlugin ()
- (void)handleGetMediaInfo:(id)arguments withResult:(FlutterResult)result;
- (void)handleGenerateThumbnail:(id)arguments withResult:(FlutterResult)result;
+ (NSString*)mimeTypeForFileAtPath: (NSString *) path;
@end

@implementation IVMediaInfoPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel methodChannelWithName:@"asia.ivity.flutter/media_info"
                                                              binaryMessenger:[registrar messenger]];
  IVMediaInfoPlugin* instance = [[IVMediaInfoPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getMediaInfo" isEqualToString:call.method]) {
    [self handleGetMediaInfo:call.arguments withResult:result];
  } else if ([@"generateThumbnail" isEqualToString:call.method]) {
    [self handleGenerateThumbnail:call.arguments withResult:result];
  } else {
    result(FlutterMethodNotImplemented);
  }
}

- (void)handleGetMediaInfo:(id)arguments withResult:(FlutterResult)result {
  NSMutableDictionary *d = [NSMutableDictionary dictionary];
  
  NSURL *mediaURL = [NSURL fileURLWithPath:arguments];
  
  NSString *mime = [IVMediaInfoPlugin mimeTypeForFileAtPath:mediaURL.path];
  
  [d setValue:mime
       forKey:@"mimeType"];
  
  if ([mime hasPrefix:@"video/"]) {
    AVURLAsset *asset = [AVURLAsset URLAssetWithURL:mediaURL options:nil];
    NSArray *tracks = [asset tracksWithMediaType:AVMediaTypeVideo];
    
    if ([tracks count] > 0) {
      AVAssetTrack *track = [tracks objectAtIndex:0];

      NSInteger width = track.naturalSize.width;
      NSInteger height = track.naturalSize.height;
      // Rotate the video by using a videoComposition and the preferredTransform
      CGAffineTransform _preferredTransform = [self fixTransform:track];
      NSInteger rotationDegrees = (NSInteger)round(radiansToDegrees(atan2(_preferredTransform.b, _preferredTransform.a)));
      if (rotationDegrees == 90 || rotationDegrees == 270) {
          width = track.naturalSize.height;
          height = track.naturalSize.width;
      }

      [d setValue:[NSNumber numberWithInteger:width]
           forKey:@"width"];
      [d setValue:[NSNumber numberWithInteger:height]
           forKey:@"height"];
      [d setValue:[NSNumber numberWithFloat:track.nominalFrameRate]
           forKey:@"frameRate"];
      [d setValue:[NSNumber numberWithLong:CMTimeGetSeconds(track.timeRange.duration) * 1000]
           forKey:@"durationMs"];
      [d setValue:[NSNumber numberWithInteger:[tracks count]]
           forKey:@"numTracks"];
    } else {
      NSLog(@"[media_info] Can not read: %@", mediaURL);
      result([FlutterError errorWithCode:@"MediaInfo" message:@"InvalidVideo" details:nil]);
      return;
    }
  } else if ([mime hasPrefix:@"audio/"]) {
      AVURLAsset *asset = [AVURLAsset URLAssetWithURL:mediaURL options:nil];
      NSArray *tracks = [asset tracksWithMediaType:AVMediaTypeAudio];
      
      if ([tracks count] > 0) {
        AVAssetTrack *track = [tracks objectAtIndex:0];
          
      [d setValue:[NSNumber numberWithLong:CMTimeGetSeconds(track.timeRange.duration) * 1000]
           forKey:@"durationMs"];

      } else {
        NSLog(@"[media_info] Can not read: %@", mediaURL);
        result([FlutterError errorWithCode:@"MediaInfo" message:@"InvalidAudio" details:nil]);
        return;
      }
  } else if ([mime hasPrefix:@"image/"]) {
    CGImageSourceRef source = CGImageSourceCreateWithURL((CFURLRef)mediaURL, NULL);
    NSDictionary* imageHeader = (__bridge NSDictionary*) CGImageSourceCopyPropertiesAtIndex(source, 0, NULL);
    
    [d setValue:[imageHeader objectForKey:@"PixelWidth"]
         forKey:@"width"];
    [d setValue:[imageHeader objectForKey:@"PixelHeight"]
         forKey:@"height"];
    
  }
  
  result(d);
}

- (void)handleGenerateThumbnail:(id)arguments withResult:(FlutterResult)result {
  NSDictionary *args = arguments;
  
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
                                      return;
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
  
}

// https://stackoverflow.com/a/5998683/375209
+ (NSString*)mimeTypeForFileAtPath: (NSString *) path {
  if (![[NSFileManager defaultManager] fileExistsAtPath:path]) {
    return nil;
  }
  // Borrowed from https://stackoverflow.com/questions/5996797/determine-mime-type-of-nsdata-loaded-from-a-file
  // itself, derived from  https://stackoverflow.com/questions/2439020/wheres-the-iphone-mime-type-database
  CFStringRef UTI = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, (__bridge CFStringRef)[path pathExtension], NULL);
  CFStringRef mimeType = UTTypeCopyPreferredTagWithClass (UTI, kUTTagClassMIMEType);
  CFRelease(UTI);
  if (!mimeType) {
    return @"application/octet-stream";
  }
  
  return CFBridgingRelease(mimeType);
}

static inline CGFloat radiansToDegrees(CGFloat radians) {
    // Input range [-pi, pi] or [-180, 180]
    CGFloat degrees = GLKMathRadiansToDegrees(radians);
    if (degrees < 0) {
        // Convert -90 to 270 and -180 to 180
        return degrees + 360;
    }
    // Output degrees in between [0, 360[
    return degrees;
};

- (CGAffineTransform)fixTransform:(AVAssetTrack*)videoTrack {
    CGAffineTransform transform = videoTrack.preferredTransform;
    // TODO(@recastrodiaz): why do we need to do this? Why is the preferredTransform incorrect?
    // At least 2 user videos show a black screen when in portrait mode if we directly use the
    // videoTrack.preferredTransform Setting tx to the height of the video instead of 0, properly
    // displays the video https://github.com/flutter/flutter/issues/17606#issuecomment-413473181
    if (transform.tx == 0 && transform.ty == 0) {
        NSInteger rotationDegrees = (NSInteger)round(radiansToDegrees(atan2(transform.b, transform.a)));
        if (rotationDegrees == 90) {
            transform.tx = videoTrack.naturalSize.height;
            transform.ty = 0;
        } else if (rotationDegrees == 270) {
            transform.tx = 0;
            transform.ty = videoTrack.naturalSize.width;
        }
    }
    return transform;
}

@end
