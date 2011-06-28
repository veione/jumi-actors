// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.jumi.core.commands;

import net.orfjackal.jumi.core.CommandListener;
import net.orfjackal.jumi.core.actors.*;

public class CommandListenerFactory implements ListenerFactory<CommandListener> {

    // TODO: remove this class and all the events, use the dynamic listeners (until code generating these classes works)

    public Class<CommandListener> getType() {
        return CommandListener.class;
    }

    public CommandListener newFrontend(MessageSender<Event<CommandListener>> target) {
        return new CommandEventToCommandListener(target);
    }

    public MessageSender<Event<CommandListener>> newBackend(CommandListener target) {
        return new CommandListenerToCommandEvent(target);
    }
}
