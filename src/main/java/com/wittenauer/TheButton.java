package com.wittenauer;

import com.google.gson.Gson;
import twitter4j.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by jwittenauer on 4/1/15.
 */
public class TheButton {
    static Date date = new Date();
    static Gson gson = new Gson();
    static int TIME_INTERVAL_MS = 1000 * 60 * 10;
    static int SECONDS_LEFT_THRESHOLD = 45;
    static Tick previousTick;
    static MovingAverage movingAverage = new MovingAverage(10 * 60);
    static DecimalFormat df = new DecimalFormat("#.##");
    public static void main(String[] args) {
        try {
            final Twitter twitter = TwitterFactory.getSingleton();
            // open websocket
            final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI("wss://wss.redditmedia.com/thebutton?h=b88627013dc3d9b28bc9bf6cbc3a6bdf78fef3c9&e=1428085713"));

            // add listener
            clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    //System.out.println(message);
                    Date messageDate = new Date();
                    Tick tick = gson.fromJson(message, Tick.class);

                    if(previousTick != null) {
                        int participantsPerSecond = 0;
                        try {
                            Number currentParticipants = NumberFormat.getNumberInstance(java.util.Locale.US).parse(tick.payload.participants_text);
                            Number previousParticipants = NumberFormat.getNumberInstance(java.util.Locale.US).parse(previousTick.payload.participants_text);
                            participantsPerSecond = currentParticipants.intValue() - previousParticipants.intValue();
                            movingAverage.newNum(participantsPerSecond);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }


                    // the button has ## seconds left with 149,173 participants and is being pressed # times per second
                    StringBuilder sb = new StringBuilder();
                    if(tick.payload.seconds_left < SECONDS_LEFT_THRESHOLD) {
                        sb.append("WARNING: ");
                        try {
                            User user = twitter.showUser(twitter.getScreenName());
                            String description = user.getDescription();
                            description.replace(String.valueOf(SECONDS_LEFT_THRESHOLD),String.valueOf(SECONDS_LEFT_THRESHOLD - 1));
                            user = twitter.updateProfile(user.getName(),user.getURL(),user.getLocation(),description);
                            System.out.println("Successfully updated the status to [" + user.getDescription()+ "].");
                        } catch (TwitterException e) {
                            e.printStackTrace();
                        }
                    }
                    sb.append("#thebutton has ").append(tick.payload.seconds_left).append(" seconds left with ").append(tick.payload.participants_text).append(" participants");
                    if(previousTick != null) {
                        sb.append(" and is being pressed ").append(df.format(movingAverage.getAvg())).append(" times per second");
                    }
                    sb.append(" @reddit");
                    if(messageDate.getTime() - date.getTime() > TIME_INTERVAL_MS
                            || tick.payload.seconds_left < SECONDS_LEFT_THRESHOLD) {
                        if(tick.payload.seconds_left < SECONDS_LEFT_THRESHOLD) {
                            SECONDS_LEFT_THRESHOLD = SECONDS_LEFT_THRESHOLD - 1;
                        } else if (messageDate.getTime() - date.getTime() > TIME_INTERVAL_MS && SECONDS_LEFT_THRESHOLD < 45) {
                            SECONDS_LEFT_THRESHOLD = SECONDS_LEFT_THRESHOLD + 1;
                        }
                        try {
                            Status status = twitter.updateStatus(sb.toString());
                            System.out.println("Successfully updated the status to [" + status.getText() + "].");
                        } catch (TwitterException e) {
                            e.printStackTrace();
                        }
                        date = new Date();
                    }
                    previousTick = tick;
                    System.out.println(sb.toString());
                }
            });

            do {
                Thread.sleep(5000);
            } while (true);

        } catch (InterruptedException ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        } catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
    }

    public class Tick {
        String type;
        Payload payload;
        public class Payload {
            String participants_text;
            String tic_mac;
            Double seconds_left;
            String now_str;
        }
    }

}
