package slackApp;

import com.google.gson.Gson;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.view.Views.*;

import com.slack.api.bolt.response.Response;
import com.slack.api.bolt.util.JsonOps;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.methods.response.views.ViewsUpdateResponse;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import com.slack.api.util.json.GsonFactory;

import java.util.HashMap;
import java.util.Map;

import static com.slack.api.model.block.element.BlockElements.*;


public class MyApp {
    public static final String category_selection_action = "category-selection-action";
    public static void main(String[] args) throws Exception {
        var app = new App();
        // All the room in the world for your code
//TODO figure out how to Publishing Messages After Modal Submissions

//        View modalView = view(v -> v
//                .type("modal")
//                .callbackId("request-modal")//meeting-arrangement
//                .title(viewTitle(title -> title.type("plain_text").text("my title").emoji(true)))
//                .submit(viewSubmit(vs -> vs.type("plain_text").text("Start")))
//                .blocks(asBlocks(
//                        section(s -> s
//                                .text(plainText("The channel we'll post the result"))
//                                .accessory(conversationsSelect(conv -> conv
//                                        .actionId("notification_conv_id")
//                                        .responseUrlEnabled(true)
//                                        .defaultToCurrentConversation(true)
//                                ))
//                        )
//                )));


        schedumeMeetingFlow(app);


        command_blockAction(app);
        addCommand(app);
        addGlobalShortcut(app);
        handleAppHomeOpenedEvent(app);

        var server = new SlackAppServer(app);
        server.start();
    }

    /**
     * type /meeting to trigger a modal where user can select type and input some text
     * when submission, check the text length
     *
     * @param app
     */
    private static void schedumeMeetingFlow(App app) {
        app.command("/meeting", (req, ctx) -> {
            System.out.println("ctx.getTriggerId() = " + ctx.getTriggerId());
            ViewsOpenResponse viewsOpenResponse = ctx.client().viewsOpen(request ->
                    request.triggerId(ctx.getTriggerId())
                            .view(buildView())
            );
            if(viewsOpenResponse.isOk()){
                return ctx.ack();
            }else{
                return Response.builder().statusCode(500).body(viewsOpenResponse.getError()).build();
            }
        });


        app.blockAction(category_selection_action, (req, ctx) -> {
            String categoryId = req.getPayload().getActions().get(0).getSelectedOption().getValue();
            View currentView = req.getPayload().getView();
            System.out.println("currentView.getId() = " + currentView.getId());

            String privateMetadata = currentView.getPrivateMetadata();
            View viewForTheCategory = buildViewByCategory(categoryId, privateMetadata);
            ViewsUpdateResponse viewsUpdateResp = ctx.client().viewsUpdate(r -> r
                    .viewId(currentView.getId())
                    .hash(currentView.getHash())
                    .view(viewForTheCategory)
            );
            return ctx.ack();
        });

        app.viewSubmission("meeting-arrangement", (req, ctx) -> {
            String privateMetadata = req.getPayload().getView().getPrivateMetadata();
            Map<String, Map<String, ViewState.Value>> stateValues = req.getPayload().getView().getState().getValues();
            String agenda = stateValues.get("agenda-block").get("agenda-action").getValue();
            Map<String, String> errors = new HashMap<>();
            if (agenda.length() <= 10) {
                errors.put("agenda-block", "Agenda needs to be longer than 10 characters.");
            }
            if (!errors.isEmpty()) {
                return ctx.ack(r -> r.responseAction("errors").errors(errors));
            } else {
                // TODO: may store the stateValues and privateMetadata
                // Responding with an empty body means closing the modal now.
                // If your app has next steps, respond with other response_action and a modal view.
                return ctx.ack();
            }
        });
    }

    /**
     * first use use command to trigger a button, then click it
     *
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

    static View buildView() {
        return view(view -> view
                .callbackId("meeting-arrangement")
                .type("modal")
                .notifyOnClose(true)
                .title(viewTitle(title -> title.type("plain_text").text("Meeting Arrangement").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .privateMetadata("{\"response_url\":\"https://hooks.slack.com/actions/T1ABCD2E12/330361579271/0dAEyLY19ofpLwxqozy3firz\"}")
                .blocks(asBlocks(
                        section(section -> section
                                .blockId("category-block")
                                .text(markdownText("Select a category of the meeting!"))
                                .accessory(staticSelect(staticSelect -> staticSelect
                                        .actionId(category_selection_action)
                                        .placeholder(plainText("Select a category"))
                                        .options(asOptions(
                                                option(plainText("Customer"), "customer"),
                                                option(plainText("Partner"), "partner"),
                                                option(plainText("Internal"), "internal")
                                        ))
                                ))
                        ),
                        input(input -> input
                                .blockId("agenda-block")
                                .element(plainTextInput(pti -> pti.actionId("agenda-action").multiline(true)))
                                .label(plainText(pt -> pt.text("Detailed Agenda").emoji(true)))
                        )
                ))
        );
    }

    static View buildViewByCategory(String categoryId, String privateMetadata) {
        Gson gson = GsonFactory.createSnakeCase();
        Map<String, String> metadata = gson.fromJson(privateMetadata, Map.class);
        metadata.put("categoryId", categoryId);
        String updatedPrivateMetadata = gson.toJson(metadata);
        return view(view -> view
                .callbackId("meeting-arrangement")
                .type("modal")
                .notifyOnClose(true)
                .title(viewTitle(title -> title.type("plain_text").text("Meeting Arrangement").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .privateMetadata(updatedPrivateMetadata)
                .blocks(asBlocks(
                        section(section -> section.blockId("category-block").text(markdownText("You've selected \"" + categoryId + "\""))),
                        input(input -> input
                                .blockId("agenda-block")
                                .element(plainTextInput(pti -> pti.actionId("agenda-action").multiline(true)))
                                .label(plainText(pt -> pt.text("Detailed Agenda").emoji(true)))
                        )
                ))
        );
    }
    static class PrivateMetadata {
        String responseUrl;
        String commandArgument;

        @Override
        public String toString() {
            return "PrivateMetadata{" +
                    "responseUrl='" + responseUrl + '\'' +
                    ", commandArgument='" + commandArgument + '\'' +
                    '}';
        }
    }
}
