package com.ego_cms.copypaste;

/*
 * #%L
 * NanoHttpd-Samples
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

class DebugServer extends NanoHTTPD {

	public DebugServer(int port) {
		super(port);
	}

	public DebugServer(String hostname, int port) {
		super(hostname, port);
	}


	public static String dumpSessionToHTML(IHTTPSession session) {
		Map<String, List<String>> decodedQueryParameters = decodeParameters(
			session.getQueryParameterString());

		StringBuilder sb = new StringBuilder();

		sb.append("<html>");
		sb.append("<head><title>Debug Server</title></head>");
		sb.append("<body>");
		sb.append("<h1>Debug Server</h1>");
		sb.append("<p><blockquote><b>URI</b> = ")
			.append(String.valueOf(session.getUri()))
			.append("<br />");
		sb.append("<b>Method</b> = ")
			.append(String.valueOf(session.getMethod()))
			.append("</blockquote></p>");
		sb.append("<h3>Headers</h3><p><blockquote>")
			.append(asString(session.getHeaders()))
			.append("</blockquote></p>");
		sb.append("<h3>Parms</h3><p><blockquote>")
			.append(asString(session.getParms()))
			.append("</blockquote></p>");
		sb.append("<h3>Parms (multi values?)</h3><p><blockquote>")
			.append(asString(decodedQueryParameters))
			.append("</blockquote></p>");

		try {
			Map<String, String> files = new HashMap<>();

			session.parseBody(files);
			sb.append("<h3>Files</h3><p><blockquote>")
				.append(asString(files))
				.append("</blockquote></p>");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		sb.append("</body>");
		sb.append("</html>");

		return sb.toString();
	}

	private static String asString(Map<String, ?> map) {
		if (map.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();

		sb.append("<ul>");
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			sb.append("<li><code><b>")
				.append(entry.getKey())
				.append("</b> = ")
				.append(entry.getValue())
				.append("</code></li>");
		}
		sb.append("</ul>");

		return sb.toString();
	}

	@Override
	public Response serve(IHTTPSession session) {
		return newFixedLengthResponse(dumpSessionToHTML(session));
	}
}
