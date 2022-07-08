package slackApp;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.view.Views.*;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;

import static com.slack.api.model.block.element.BlockElements.*;


public class MyApp {
    public static void main(String[] args) throws Exception {
        var app = new App();
        // All the room in the world for your code
        command_blockAction(app);

        addCommand(app);
        addGlobalShortcut(app);
        handleAppHomeOpenedEvent(app);

        var server = new SlackAppServer(app);
        server.start();
    }

    /**
     * first use use command to trigger a button, then click it
     * @param app
     */
    private static void command_blockAction(App app) {
        app.command("/ping", (req, ctx) -> {
            return ctx.ack(asBlocks(
                    section(section -> section.text(markdownText(":wave: pong"))),
                    actions(actions -> actions
                            .elements(asElements(
                                    button(b -> b.actionId("ping-again").text(plainText(pt -> pt.text("Ping"))).value("ping"))
                            ))
                    )
            ));
        });

        app.blockAction("ping-again", (req, ctx) -> {
            String value = req.getPayload().getActions().get(0).getValue(); // "button's value"
            if (req.getPayload().getResponseUrl() != null) {
                // Post a message to the same channel if it's a block in a message
                ctx.respond("You've sent " + value + " by clicking the button!");
            }
            return ctx.ack();
        });
    }

    private static void handleAppHomeOpenedEvent(App app) {
        app.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            System.out.println("payload = " + payload);
            System.out.println("ctx = " + ctx);
            var appHomeView = view(view -> view
                    .type("home")
                    .blocks(asBlocks(
                            section(section -> section.text(markdownText(mt -> mt.text("*Welcome to your _App's Home_* :tada:")))),
                            divider(),
                            section(section -> section.text(markdownText(mt -> mt.text("This button won't do much for now but you can set up a listener for it using the `actions()` method and passing its unique `action_id`. See an example on <https://slack.dev/java-slack-sdk/guides/interactive-components|slack.dev/java-slack-sdk>.")))),
                            actions(actions -> actions
                                    .elements(asElements(
                                            button(b -> b.text(plainText(pt -> pt.text("Click me!"))).value("button1").actionId("button_1"))
                                    ))
                            )
                    ))
            );

            var res = ctx.client().viewsPublish(r -> r
                    .userId(payload.getEvent().getUser())
                    .view(appHomeView)
            );

            return ctx.ack();
        });
    }

    private static void addGlobalShortcut(App app) {
        app.globalShortcut("callback-id", (req, ctx) -> {
            // Using the defualt singleton thread pool
            app.executorService().submit(() -> {
                // Do anything asynchronously here
                try {
                    ctx.client().viewsOpen(r -> r
                            .triggerId(ctx.getTriggerId())
                            .view(View.builder().build())
                    );
                } catch (Exception e) {
                    // Error handling
                }
            });
            // This line will be synchrously executed
            return ctx.ack();
        });
    }

    private static void addCommand(App app) {
        app.command("/hello", (req, ctx) -> {
            return ctx.ack(":wave: Hello!");
        });
    }
}
