package org.openintents.safe;

import android.content.Context;

import org.openintents.safe.model.PassEntry;
import org.openintents.safe.model.Passwords;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

public class Export {
    static void exportDatabaseToWriter(Context context, Writer w) throws IOException {

            CSVWriter writer = new CSVWriter(w, ',');

            String[] header = {context.getString(R.string.category),
                    context.getString(R.string.description),
                    context.getString(R.string.website),
                    context.getString(R.string.username),
                    context.getString(R.string.password),
                    context.getString(R.string.notes),
                    context.getString(R.string.last_edited)
            };
            writer.writeNext(header);

            HashMap<Long, String> categories = Passwords.getCategoryIdToName();

            List<PassEntry> rows;
            rows = Passwords.getPassEntries(Long.valueOf(0), true, false);

            for (PassEntry row : rows) {
                String[] rowEntries = {categories.get(row.category),
                        row.plainDescription,
                        row.plainWebsite,
                        row.plainUsername,
                        row.plainPassword,
                        row.plainNote,
                        row.lastEdited
                };
                writer.writeNext(rowEntries);
            }
            writer.close();
    }
}
