package net.sf.l2j.commons.logging.formatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogRecord;

import net.sf.l2j.commons.logging.MasterFormatter;

public class ConsoleLogFormatter extends MasterFormatter
{
	@Override
	public String format(LogRecord record)
	{
		final StringWriter sw = new StringWriter();
		sw.append("[" + String.format("%1$tF %1$TT", System.currentTimeMillis()) + "] ");
		sw.append(record.getMessage());
		sw.append(CRLF);

		final Throwable throwable = record.getThrown();
		if (throwable != null)
		{
			throwable.printStackTrace(new PrintWriter(sw));
		}

		return sw.toString();
	}
}