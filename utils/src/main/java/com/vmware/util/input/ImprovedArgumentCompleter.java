package com.vmware.util.input;

import static jline.internal.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.internal.Log;

public class ImprovedArgumentCompleter extends ArgumentCompleter {

    /**
     * Create a new completer with the specified argument delimiter.
     *
     * @param delimiter     The delimiter for parsing arguments
     * @param completers    The embedded completers
     */
    public ImprovedArgumentCompleter(final ArgumentDelimiter delimiter, final Collection<Completer> completers) {
        super(delimiter, completers);
    }

    /**
     * Create a new completer with the specified argument delimiter.
     *
     * @param delimiter     The delimiter for parsing arguments
     * @param completers    The embedded completers
     */
    public ImprovedArgumentCompleter(final ArgumentDelimiter delimiter, final Completer... completers) {
        super(delimiter, completers);
    }

    /**
     * Create a new completer with the default {@link WhitespaceArgumentDelimiter}.
     *
     * @param completers    The embedded completers
     */
    public ImprovedArgumentCompleter(final Completer... completers) {
        super(completers);
    }

    @Override
    public int complete(String buffer, final int cursor, final List<CharSequence> candidates) {
        // candidates can be null
        checkNotNull(candidates);

        if (buffer == null) {
            buffer = "";
        }
        ArgumentDelimiter delim = getDelimiter();
        ArgumentList list = delim.delimit(buffer, cursor);
        int argpos = list.getArgumentPosition();
        int argIndex = list.getCursorArgumentIndex();

        if (argIndex < 0) {
            return -1;
        }

        List<Completer> completers = getCompleters();
        Completer completer;

        // if we are beyond the end of the completers, just use the last one
        if (argIndex >= completers.size()) {
            completer = completers.get(completers.size() - 1);
        }
        else {
            completer = completers.get(argIndex);
        }

        // ensure that all the previous completers are successful before allowing this completer to pass (only if strict).
        for (int i = 0; isStrict() && (i < argIndex); i++) {
            Completer sub = completers.get(i >= completers.size() ? (completers.size() - 1) : i);
            String[] args = list.getArguments();
            String arg = (args == null || i >= args.length) ? "" : args[i];

            List<CharSequence> subCandidates = new LinkedList<CharSequence>();
            if (sub instanceof ArgumentListAware) {
                ((ArgumentListAware) sub).setArgumentList(list);
            }

            if (sub.complete(arg, arg.length(), subCandidates) == -1) {
                return -1;
            }

            if (subCandidates.size() == 0) {
                return -1;
            }
        }

        if (completer instanceof ArgumentListAware) {
            ((ArgumentListAware) completer).setArgumentList(list);
        }

        int ret = completer.complete(list.getCursorArgument(), argpos, candidates);
        if (ret == -1) {
            return -1;
        }

        int pos = ret + list.getBufferPosition() - argpos;

        // Special case: when completing in the middle of a line, and the area under the cursor is a delimiter,
        // then trim any delimiters from the candidates, since we do not need to have an extra delimiter.
        //
        // E.g., if we have a completion for "foo", and we enter "f bar" into the buffer, and move to after the "f"
        // and hit TAB, we want "foo bar" instead of "foo  bar".

        if ((cursor != buffer.length()) && delim.isDelimiter(buffer, cursor)) {
            for (int i = 0; i < candidates.size(); i++) {
                CharSequence val = candidates.get(i);

                while (val.length() > 0 && delim.isDelimiter(val, val.length() - 1)) {
                    val = val.subSequence(0, val.length() - 1);
                }

                candidates.set(i, val);
            }
        }

        Log.trace("Completing ", buffer, " (pos=", cursor, ") with: ", candidates, ": offset=", pos);

        return pos;
    }
}
