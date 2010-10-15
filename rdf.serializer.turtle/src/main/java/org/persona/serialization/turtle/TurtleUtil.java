package org.persona.serialization.turtle;

import org.persona.middleware.PResource;
import org.persona.middleware.TypeMapper;
import org.persona.middleware.util.StringUtils;

/**
 * @author mtazari
 * 
 */
public class TurtleUtil {

	static String xmlLiteral = PResource.RDF_NAMESPACE + "XMLLiteral";
	static TypeMapper typeMapper = null;

	static String decodeString(String s) {
		int backSlashIdx = s.indexOf('\\');

		if (backSlashIdx == -1) {
			// No escaped characters found
			return s;
		}

		int startIdx = 0;
		int sLength = s.length();
		StringBuffer sb = new StringBuffer(sLength);

		while (backSlashIdx != -1) {
			sb.append(s.substring(startIdx, backSlashIdx));

			if (backSlashIdx + 1 >= sLength) {
				throw new IllegalArgumentException("Unescaped backslash in: "
						+ s);
			}

			char c = s.charAt(backSlashIdx + 1);

			if (c == 't') {
				sb.append('\t');
				startIdx = backSlashIdx + 2;
			} else if (c == 'r') {
				sb.append('\r');
				startIdx = backSlashIdx + 2;
			} else if (c == 'n') {
				sb.append('\n');
				startIdx = backSlashIdx + 2;
			} else if (c == '"') {
				sb.append('"');
				startIdx = backSlashIdx + 2;
			} else if (c == '>') {
				sb.append('>');
				startIdx = backSlashIdx + 2;
			} else if (c == '\\') {
				sb.append('\\');
				startIdx = backSlashIdx + 2;
			} else if (c == 'u') {
				// \\uxxxx
				if (backSlashIdx + 5 >= sLength) {
					throw new IllegalArgumentException(
							"Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);

				try {
					c = (char) Integer.parseInt(xx, 16);
					sb.append(c);

					startIdx = backSlashIdx + 6;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(
							"Illegal Unicode escape sequence '\\u" + xx
									+ "' in: " + s);
				}
			} else if (c == 'U') {
				// \\Uxxxxxxxx
				if (backSlashIdx + 9 >= sLength) {
					throw new IllegalArgumentException(
							"Incomplete Unicode escape sequence in: " + s);
				}
				String xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);

				try {
					c = (char) Integer.parseInt(xx, 16);
					sb.append(c);

					startIdx = backSlashIdx + 10;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(
							"Illegal Unicode escape sequence '\\U" + xx
									+ "' in: " + s);
				}
			} else {
				throw new IllegalArgumentException("Unescaped backslash in: "
						+ s);
			}

			backSlashIdx = s.indexOf('\\', startIdx);
		}

		sb.append(s.substring(startIdx));

		return sb.toString();
	}

	static String encodeLongString(String s) {
		// TODO: not all double quotes need to be escaped. It suffices to encode
		// the ones that form sequences of 3 or more double quotes, and the ones
		// at the end of a string.
		s = globalReplaceChar('\\', "\\\\", s);
		s = globalReplaceChar('"', "\\\"", s);
		return s;
	}

	static String encodeString(String s) {
		s = globalReplaceChar('\\', "\\\\", s);
		s = globalReplaceChar('\t', "\\t", s);
		s = globalReplaceChar('\n', "\\n", s);
		s = globalReplaceChar('\r', "\\r", s);
		s = globalReplaceChar('"', "\\\"", s);
		return s;
	}

	static String encodeURIString(String s) {
		s = globalReplaceChar('\\', "\\\\", s);
		s = globalReplaceChar('>', "\\>", s);
		return s;
	}

	static int findURISplitIndex(String uri) {
		int uriLength = uri.length();

		int idx = uriLength - 1;

		// Search last character that is not a name character
		for (; idx >= 0; idx--) {
			if (!TurtleUtil.isNameChar(uri.charAt(idx))) {
				// Found a non-name character
				break;
			}
		}

		idx++;

		// Local names need to start with a 'nameStartChar', skip characters
		// that are not nameStartChar's.
		for (; idx < uriLength; idx++) {
			if (TurtleUtil.isNameStartChar(uri.charAt(idx))) {
				break;
			}
		}

		if (idx > 0 && idx < uriLength) {
			// A valid split index has been found
			return idx;
		}

		// No valid local name has been found
		return -1;
	}

	private static String globalReplaceChar(char c, String rpl, String input) {
		char aux;
		int n = input.length();
		StringBuffer sb = new StringBuffer(n << 1);
		for (int i = 0; i < n; i++) {
			aux = input.charAt(i);
			if (c == aux)
				sb.append(rpl);
			else
				sb.append(aux);
		}
		return sb.toString();
	}

	static boolean isLanguageChar(int c) {
		return StringUtils.isAsciiLetter((char) c)
				|| StringUtils.isDigit((char) c) || c == '-';
	}

	static boolean isLanguageStartChar(int c) {
		return StringUtils.isAsciiLetter((char) c);
	}

	static boolean isNameChar(int c) {
		return isNameStartChar(c) || StringUtils.isDigit((char) c) || c == '-'
				|| c == 0x00B7 || c >= 0x0300 && c <= 0x036F || c >= 0x203F
				&& c <= 0x2040;
	}

	static boolean isNameStartChar(int c) {
		return c == '_' || isPrefixStartChar(c);
	}

	static boolean isPrefixChar(int c) {
		return isNameChar(c);
	}

	static boolean isPrefixStartChar(int c) {
		return StringUtils.isAsciiLetter((char) c) || c >= 0x00C0
				&& c <= 0x00D6 || c >= 0x00D8 && c <= 0x00F6 || c >= 0x00F8
				&& c <= 0x02FF || c >= 0x0370 && c <= 0x037D || c >= 0x037F
				&& c <= 0x1FFF || c >= 0x200C && c <= 0x200D || c >= 0x2070
				&& c <= 0x218F || c >= 0x2C00 && c <= 0x2FEF || c >= 0x3001
				&& c <= 0xD7FF || c >= 0xF900 && c <= 0xFDCF || c >= 0xFDF0
				&& c <= 0xFFFD || c >= 0x10000 && c <= 0xEFFFF;
	}

	static boolean isWhitespace(int c) {
		// Whitespace character are space, tab, newline and carriage return:
		return c == 0x20 || c == 0x9 || c == 0xA || c == 0xD;
	}
}
