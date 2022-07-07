## instruction
https://api.slack.com/start/building/bolt-java

gradle dependency is copied from: https://slack.dev/java-slack-sdk/guides/getting-started-with-bolt#gradle

## enable hello command 
code refer to MyApp
setup refer to: https://slack.dev/java-slack-sdk/guides/getting-started-with-bolt#enable-hello-command

### high level steps to add a new command 
1. add code 

```java
    app.command("/hello", (req, ctx) -> {
        return ctx.ack(":wave: Hello!");
    });
```

2. add Slash Command in Slack app config page
https://slack.dev/java-slack-sdk/guides/getting-started-with-bolt#enable-hello-command
eg:
Command: /hello
Request URL: https://97f1-130-41-63-67.ngrok.io/slack/events
Short Description: anything 

if this is the first time to enable Slash command, you need to reinstall Slack bot


