/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Stephen Connolly
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.scm.browsers;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionRepositoryBrowser;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.util.FormValidation;
import hudson.util.FormValidation.URLCheck;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;

/**
 * {@link RepositoryBrowser} for Sventon 2.x.
 * 
 * @author Stephen Connolly
 */
public class AGFASventon extends SubversionRepositoryBrowser {

	private final String url = "http://wtlswtom01.aqua.mitra.com/svn";

	private static Pattern SVN_REPO_PATTERN = Pattern.compile("http://.*/svn/repos/(.*?)/.*");

	/**
	 * The charset to use when encoding paths in an URI (specified in RFC 3986).
	 */
	protected static final String URL_CHARSET = "UTF-8";

	@DataBoundConstructor
	public AGFASventon() {
	}

	private String getRepositoryURL(String repositoryRoot) {
		if (repositoryRoot != null) {
			String name = repositoryRoot.substring(repositoryRoot.lastIndexOf('/') + 1);
			return url + "/repos/" + name;
		}
		
		
		//TODO do we need the rest ?
		Ancestor ancestor = Stapler.getCurrentRequest().findAncestor(AbstractProject.class);
		if (ancestor == null)
			return null;
		
		AbstractProject project = (AbstractProject) ancestor.getObject();
		SubversionSCM scm = (SubversionSCM) project.getScm();
		
		String location = scm.getLocations()[0].getURL();
		Matcher m = SVN_REPO_PATTERN.matcher(location);
		if (m.matches()) {
			return url + "/repos/" + m.group(1);
		}
		
		return null;
	}

	@Override
	public URL getDiffLink(Path path) throws IOException {
		if (path.getEditType() != EditType.EDIT)
			return null; // no diff if this is not an edit change
		String repositoryRoot = getRepositoryURL(path.getLogEntry().getRepositoryRoot());
		if (repositoryRoot == null) {
			return null;
		}
		int r = path.getLogEntry().getRevision();
		return new URL(String.format("%s/diff/%s?revision=%d", repositoryRoot,
				encodePath(getPath(path)), r));
	}

	@Override
	public URL getFileLink(Path path) throws IOException {
		String repositoryRoot = getRepositoryURL(path.getLogEntry().getRepositoryRoot());
		if (repositoryRoot == null) {
			return null;
		}
		if (path.getEditType() == EditType.DELETE)
			return null; // no file if it's gone
		int r = path.getLogEntry().getRevision();
		return new URL(String.format("%s/goto/%s?revision=%d", repositoryRoot,
				encodePath(getPath(path)), r));
	}

	/**
	 * Trims off the root module portion to compute the path within FishEye.
	 */
	private String getPath(Path path) {
		String s = trimHeadSlash(path.getValue());
		return s;
	}

	private static String encodePath(String path) throws UnsupportedEncodingException {
		StringBuilder buf = new StringBuilder();
		if (path.startsWith("/")) {
			buf.append('/');
		}
		boolean first = true;
		for (String pathElement : path.split("/")) {
			if (first) {
				first = false;
			} else {
				buf.append('/');
			}
			buf.append(URLEncoder.encode(pathElement, URL_CHARSET));
		}
		if (path.endsWith("/")) {
			buf.append('/');
		}
		return buf.toString().replace("%20", "+");
	}

	@Override
	public URL getChangeSetLink(LogEntry changeSet) throws IOException {
		String repositoryRoot = getRepositoryURL(changeSet.getRepositoryRoot());
		if (repositoryRoot == null) {
			return null;
		}
		return new URL(String.format("%s/info?revision=%d", repositoryRoot,
				changeSet.getRevision()));
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
		public String getDisplayName() {
			return "AGFA Sventon";
		}

	}

	private static final long serialVersionUID = 1L;
}
