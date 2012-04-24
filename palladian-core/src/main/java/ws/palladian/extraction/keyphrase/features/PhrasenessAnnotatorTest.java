//package ws.palladian.extraction.keyphrase.features;
//
//import java.io.File;
//import java.util.List;
//
//import org.junit.Test;
//
//import ws.palladian.extraction.PipelineDocument;
//import ws.palladian.extraction.ProcessingPipeline;
//import ws.palladian.extraction.feature.DuplicateTokenRemover;
//import ws.palladian.extraction.feature.LengthTokenRemover;
//import ws.palladian.extraction.feature.NGramCreator;
//import ws.palladian.extraction.feature.RegExTokenRemover;
//import ws.palladian.extraction.feature.StemmerAnnotator;
//import ws.palladian.extraction.feature.StemmerAnnotator.Mode;
//import ws.palladian.extraction.feature.StopTokenRemover;
//import ws.palladian.extraction.feature.TokenMetricsCalculator;
//import ws.palladian.extraction.token.RegExTokenizer;
//import ws.palladian.extraction.token.TokenizerInterface;
//import ws.palladian.helper.constants.Language;
//import ws.palladian.helper.io.FileHelper;
//import ws.palladian.model.features.Annotation;
//import ws.palladian.model.features.AnnotationFeature;
//import ws.palladian.model.features.FeatureVector;
//import ws.palladian.model.features.NumericFeature;
//
//public class PhrasenessAnnotatorTest {
//    @Test
//    public void testPhrasenessAnnotator() {
//        // String text = FileHelper.readFileToString(new File("/Users/pk/Desktop/Snow-WhiteandRose-Red.txt"));
//        // String text = FileHelper.readFileToString(new File("/Users/pk/Desktop/temp/44.txt"));
//        String text = "Shawn Blanc Writer (shawnblanc.net) Posted Apr 18, 2012 in mac writer  Who are you, and what do you do? My name is Shawn Blanc. I write a tech and design-centric website, drink a cup of coffee every day, and love to snowboard.  What hardware are you using? I spend most of my work day sitting in front of a 23-inch Apple Cinema Display. It’s the older, aluminum-style Cinema Display. You know, the nice one with the matte screen.  Pushing the pixels to the ACD is a 13-inch MacBook Air. It’s the mid-2011 model and it’s maxed out with all the bells and whistles that they came with at the time: a 1.8 GHz Core i7 processor, a 256 GB solid state drive, and 4 GBs of memory.  Unless I’m undocked from the Cinema Display, the MacBook Air is in clamshell mode. I have a Book Arq from 12 South that props it up in the air (no pun intended). The Book Arq simultaneously helps to keep the laptop cool and while also taking up a smaller footprint on my desk. I have a great big desk that I made by hand in my garage a couple summers ago. And even though it has over 21 square feet of nerd real estate, I like to keep the least amount of stuff on that acreage as possible. A clean and open work space makes it easier for me to think and work in peace.  In front of the Cinema Display is one of Apple’s slim, bluetooth keyboards and a Magic Trackpad. I used to have the full-sized aluminum wired keyboard and a Magic Mouse, but when I started using Lion and all of its fancy multi-finger gestures I switched to the Magic Trackpad, and my previous keyboard’s numbered pad made the trackpad seem too far away so I downsized to the Bluetooth keyboard.  Behind the Cinema Display is where I store my Blue Yeti microphone. It’s USB powered and I use it every day to record my daily, members-only show, Shawn Today.  Sitting on my desk next to the Cinema Display are an iPad 2 and iPhone 4S. Both black. Both the base model. The iPad has a Smart cover, the iPhone has no case at all.  And what software? Between my iPhone, iPad, and Mac, the number of apps I use on a daily basis makes up a list longer than my arm. Here’s the hot list, in ascending order based on device screen size.  On the iPhone:  Tweetbot: My Twitter client of choice. It has a very unique design look, but I like it. Moreover, it’s a Twitter Power User’s dream of an app. It has a slew of features and you can get a lot of Twitter fun out of it.  Instagram: I enjoy this dorky-but-fun camera app slash social network. There’s just something fun about taking pictures and applying cheesy filters to them.  Mail: I check my email much too often via my iPhone.  OmniFocus: I use OmniFocus on my iPhone, iPad, and Mac. On the iPhone version I mostly use it for tapping in a to-do item when on the go. The best version of OmniFocus is, by far, on the iPad. But here I am getting ahead of myself.  Simplenote: The utility app I use on my iPhone more than any other. Simplenote syncs its text files between my iPad and (via Notational Velocity) my Mac. I jot down lots of notes (some very important, others not so much) in Simplenote and adore the ubiquity of my notes that it allows me.  Siri: A newcomer to iOS, but Siri is a fantastic feature of the iPhone 4S. I use it regularly for adding quick reminders, creating timers and alarms, and for dictating emails and text messages. When Siri was first introduced there was a lot of hubbub about if it would be just a cool gimmick that eventually faded away or if it would stick around, and it’s clear now that Siri is much more than a gimmick.  On the iPad:  Instapaper: Of course. In some ways, my iPad is Instaper. I read a lot of news and blog articles as part of my job and the majority of them get fed through Instapaper.  OmniFocus: Like I mentioned above, OmniFocus on the iPad is the best of this three-app suite. I use the iPad version regularly for reviewing my tasks and for managing my day’s to-do list.  Simplenote: When I am writing on my iPad, it almost certainly via Simplenote. It has a full-screen mode that’s just lovely and, like I mentioned above, it syncs to my phone and my Mac.  Safari: Surfing the web on the iPad is great. Safari gets a lot of use when I’m relaxing on the couch with the iPad in one hand and a cup of coffee in the other. (Okay, you caught me. That’s not entirely true. In reality, the cup of coffee is on the table next to me because the iPad cannot easily be held with one hand.)  On the Mac:  MarsEdit: This is, by far, my most used app on my Mac. It is at the center of my blogging workflow and I am thankful for it. Each day I usually publish several links of cool or interesting things to my website. With MarsEdit there is a Javascript Bookmarklet that I keep in Safari. When I am on a Web page that I want to link to, I click the bookmarklet and it will open up MarsEdit with much of the information ready to go.  I also regularly write long-form articles. sometimes these get written in MarsEdit, but sometimes they’ve been written in another app. But regardless of where they originated, once they’re ready they get previewed and published to my website via MarsEdit.  Byword: When I am doing a long form article or writing something of substance, I often will write in Byword. In fact, I am writing my answers to this nerdy interview in Byword right now. It’s a nice and simple app, with just a few preferences. It has a light and dark mode and it can go full-screen in Lion.  LaunchBar: Without LaunchBar I’m handicapped on a Mac. I Command+Space for just about everything.  Keyboard Maestro: Another fine utility app that is a power user’s dream. With Keyboard Maestro I can apply global keyboard shortcuts that can then be used to trigger just about any sort of macro, action, or script that I want. I have one set to launch Mail upon a particular keystroke. As well, I use Keyboard Maestro for doing bottom-posting replies in Mail. I use it for adding notes from certain apps into Yojimbo, and I use it to toggle the audio output on my Mac. And that’s just the tip of the Keyboard Maestro iceburg.  Dropbox: I’ve just got the one MacBook Air, so I don’t have a need to sync files and folders across multiple computers. And so I use Dropbox as a sort of “real-time backup app”. All the projects I’m currently working on exist in my Dropbox folder. Which means the second I hit save, that version of the file is instantly uploaded.  Transmit: This world-class FTP client is how I toss all my weblog images, podcast audio files, and whatever else into my Amazon S3 account. I also use it for any and all times I need FTP access to my websites.  Coda: Speaking of websites, I do all my web development and amateur coding in Coda. It is fast, light-weight, and has that top-notch Panic design aesthetic. I’ve been using it since it first shipped in the spring of 2007. They are working on version 2.0 now, and I can’t wait to see what they drum up.  Safari: I spend a lot of my time on the Web, reading and researching, and Safari is my Web browser of choice.  Yojimbo: This is my ”Anything Bucket”. I toss all my digital receipts, bookmarks, inspirational quotes, and whatever else into this fine application. I have several custom AppleScripts that tie in with Yojimbo so it’s easy for me to toss items from Safari and Mail into the app.  What would be your dream setup? All things considered, I pretty much do have my dream setup. In my day-to-day, I rarely ever feel any lack in the hardware or software I use.  For when I travel, I would love to see the MacBook Air get a little more iPadified by getting longer battery life and built in, affordable, 3G connectivity. And I guess for my desk I’d also love to see a matte version of the 27-inch Apple Thunderbolt Display.  Other than that, I think I might like a better keyboard. I’ve never thought anything bad about the slim Apple bluetooth keyboard I use, but recently I spent some time using my cousin’s mechanical keyboard and there was a completely different feel to it. I’ve never been a keyboard snob, but considering my profession, perhaps the time to get snobby about keyboards has come.";
//        ProcessingPipeline pipeline = new ProcessingPipeline();
//        pipeline.add(new RegExTokenizer());
//        pipeline.add(new StopTokenRemover(Language.ENGLISH));
//        pipeline.add(new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY));
//        pipeline.add(new LengthTokenRemover(3));
//        pipeline.add(new RegExTokenRemover("[^A-Za-z]+"));
//        pipeline.add(new NGramCreator(4));
//        pipeline.add(new TokenMetricsCalculator());
//        pipeline.add(new PhrasenessAnnotator(0.2));
//        pipeline.add(new DuplicateTokenRemover());
//        PipelineDocument document = pipeline.process(new PipelineDocument(text));
//        AnnotationFeature annotationFeature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
//        List<Annotation> annotations = annotationFeature.getValue();
//        for (Annotation annotation : annotations) {
//            FeatureVector annotationFv = annotation.getFeatureVector();
//            Double numWords = annotationFv.get(TokenMetricsCalculator.WORD_LENGTH).getValue();
//            NumericFeature phraseness = (NumericFeature)annotation.getFeatureVector().get(PhrasenessAnnotator.GENERALIZED_DICE);
//            //if (numWords > 1) {
//                if (phraseness.getValue() < 0.1) {
//                    continue;
//                }
//                System.out.println(annotation.getValue() + "|" + phraseness);
//            //}
//            //if (numWords == 1) {
//            //    System.out.println(annotation.getValue() + "|" + phraseness);
//            //}
//        }
//        
//    }
//
//}
