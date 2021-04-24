#import "NotificationSounds.h"
#import <AudioToolbox/AudioToolbox.h>

@implementation NotificationSounds

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(sampleMethod:(NSString *)stringArgument numberParameter:(nonnull NSNumber *)numberArgument callback:(RCTResponseSenderBlock)callback)
{
    // TODO: Implement some actually useful functionality
	callback(@[[NSString stringWithFormat: @"numberArgument: %@ stringArgument: %@", numberArgument, stringArgument]]);
}

RCT_REMAP_METHOD(getNotifications, soundTypeParamentr:(NSString *)soundType loadSoundsWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    NSMutableArray *directoryURLList = [[NSMutableArray alloc] init];

    if ([soundType isEqualToString:@"notification"]) {
        [directoryURLList addObject:[[NSBundle mainBundle] bundleURL]];
        [directoryURLList addObject:[NSURL URLWithString:@"/Library/Sounds"]];
        // the location of default sound
        [directoryURLList addObject:[NSURL URLWithString:@"/System/Library/PrivateFrameworks/ActionKit.framework"]];
    } else if ([soundType isEqualToString:@"ringtone"]) {
        [directoryURLList addObject:[NSURL URLWithString:@"/Library/Ringtones"]];
    } else {
        [directoryURLList addObject:[NSURL URLWithString:@"/System/Library/Audio/UISounds"]];
    }

    NSMutableArray *audioFileList = [[NSMutableArray alloc] init];

    NSFileManager *fileManager = [[NSFileManager alloc] init];
    NSArray *keys = [NSArray arrayWithObject:NSURLIsDirectoryKey];
    // https://developer.apple.com/documentation/usernotifications/unnotificationsound?language=objc
    NSArray *supportedFormats = [[NSArray alloc] initWithObjects:@"caf", @"aiff", @"m4r", @"wav", nil];

    for (NSURL *directoryURL in directoryURLList) {
        NSDirectoryEnumerator *enumerator = [fileManager
                                             enumeratorAtURL:directoryURL
                                             includingPropertiesForKeys:keys
                                             options:(NSDirectoryEnumerationSkipsSubdirectoryDescendants
                                                    | NSDirectoryEnumerationSkipsPackageDescendants
                                                    | NSDirectoryEnumerationSkipsHiddenFiles)
                                             errorHandler:^(NSURL *url, NSError *error) {
                                                 // Handle the error.
                                                 // Return YES if the enumeration should continue after the error.
                                                 return YES;
                                             }];

        for (NSURL *url in enumerator) {
            NSError *error;
            NSNumber *isDirectory = nil;

            if (! [url getResourceValue:&isDirectory forKey:NSURLIsDirectoryKey error:&error]) {
                // handle error
            }
            else if (! [isDirectory boolValue]) {
                NSString* fileName = [NSString stringWithFormat:@"%@", url.lastPathComponent];
                NSMutableArray *titles = [NSMutableArray arrayWithArray: [fileName componentsSeparatedByString:@"."]];

                if ([titles count] > 1) {
                    NSString *extension = [[titles lastObject] lowercaseString];
                    if ([supportedFormats containsObject:(extension)]) {
                        // remove the extension
                        [titles removeLastObject];
                        // re-join the file names to title
                        NSString *soundTitle = [titles componentsJoinedByString:@"."];

                        // maybe no need these, it just uses file name as title
//                         NSCharacterSet *notAllowedChars = [[NSCharacterSet alphanumericCharacterSet] invertedSet];
//                         NSString *soundTitle = [[soundTitle componentsSeparatedByCharactersInSet:notAllowedChars] componentsJoinedByString:@" "];

                        NSLog (@"Result: %@", soundTitle);
                        CFURLRef cfUrl = (__bridge CFURLRef)url;
                        SystemSoundID soundID;
                        AudioServicesCreateSystemSoundID(cfUrl, &soundID);

                        NSString *urlString = url.absoluteString;
                        NSMutableDictionary *audioSound = [NSMutableDictionary dictionary];
                        [audioSound setObject: soundTitle  forKey: @"title"];
                        [audioSound setObject: urlString  forKey:  @"url"];
                        [audioSound setObject: [NSNumber numberWithInt:((int)soundID)]  forKey:  @"soundID"];
                        [audioFileList addObject:audioSound];
                    }
                }
            }
        }
    }

    // NSLog(@"URL: %@", audioFileList);
    if (audioFileList) {
        resolve(audioFileList);
    } else {
        NSError *error = {@"NO data found"};
        reject(@"error", @"error description", error);
    }
}


SystemSoundID soundID = 0;

RCT_EXPORT_METHOD(playSample:(NSString *) soundUrl
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSURL *url = [NSURL URLWithString:soundUrl];
    AudioServicesDisposeSystemSoundID(soundID);
    //Register the sound to the system
    AudioServicesCreateSystemSoundID((__bridge CFURLRef)url, &soundID);
    AudioServicesPlaySystemSound(soundID);
    AudioServicesPlaySystemSoundWithCompletion(soundID, ^{
        AudioServicesRemoveSystemSoundCompletion(soundID);
        AudioServicesDisposeSystemSoundID(soundID);
        resolve(@YES);
    });
}

RCT_EXPORT_METHOD(stopSample:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    AudioServicesRemoveSystemSoundCompletion(soundID);
    AudioServicesDisposeSystemSoundID(soundID);
    resolve(@YES);
}


@end
