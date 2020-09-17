<%@ include file="includes/header.jsp" %>
<%@ page import="java.util.*, java.text.*" language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<p>
	<b>Trigger Event (to POIS / Transcoder)</b>
</p>
<%
	String activeTab	= request.getParameter("activeTab");
	if(activeTab == null)	{
		activeTab = "home";
	}

	StringBuffer queryStringBuf	= new StringBuffer();
	if(request.getParameter("activeTab") != null) {
		queryStringBuf.append("&activeTab="+request.getParameter("activeTab"));
	}
%>
<style>
	ul {
		list-style:none;
		margin:0;
		padding:0; 
	} 
	
	ul li { 
		text-decoration:none;
		float:left;
		margin-right:5px;
		border:1px solid #000;
		border-bottom:none;
		padding:5px;
		height:20px;
		text-align:center;
		display:block;
		color:black;
		background:#99CC66;
		-moz-border-radius-topleft:5px;
		-moz-border-radius-topright:5px;
		-webkit-border-top-left-radius:5px;
		-webkit-border-top-right-radius:5px; 
	}

	ul li:hover {
		color:white;
		background:#469438;
	}
	ul li#<%= activeTab %> {
		color:white;
		background:#469438;
	}
	a {
		text-decoration: none;
		color:black;
		font-size: 12px;
		font-weight: bold;
	}
</style>
	<table cellpadding=0px cellspacing=0px border=0px>
		<tr>
			<td>
				<ul>
					<li id="home"><a href="trigger_ap_event.jsp?activeTab=home">Home</a></li>
					<li id="ccms"><a href="trigger_ap_event.jsp?activeTab=ccms">Upload CCMS schedule</a></li>
					<li id="alternate"><a href="trigger_ap_event.jsp?activeTab=alternate">Alternate Program</a></li>
					<li id="signal"><a href="trigger_ap_event.jsp?activeTab=signal">Signal Processing Event</a></li>
					<li id="manifest"><a href="trigger_ap_event.jsp?activeTab=manifest">Manifest Confirmation Event</a></li>
					<li id="feed"><a href="trigger_ap_event.jsp?activeTab=feed">Acquisition Point Feed</a></li>
				</ul>
			</td>
		</tr><tr>
			<td>
				<table cellpadding=5 cellspacing=1 border=0 height=200px width=800px style="background-color: black;">
					<tr valign=top style="background-color: white;">
						<td>
							<% if("home".equals(activeTab)) { %>
								<%@ include file="tae_home.jsp" %>
							<% } %>
							<% if("ccms".equals(activeTab)) { %>
								<%@ include file="tae_ccms_schedule.jsp" %>
							<% } %>
							<% if("alternate".equals(activeTab)) { %>
								<%@ include file="tae_alternate_program.jsp" %>
							<% } %>
							<% if("signal".equals(activeTab)) { %>
								<%@ include file="tae_signal_event.jsp" %>
							<% } %>
							<% if("manifest".equals(activeTab)) { %>
								<%@ include file="tae_manifest_event.jsp" %>
							<% } %>
							<% if("feed".equals(activeTab)) { %>
								<%@ include file="tae_feed.jsp" %>
							<% } %>
						</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
<%@ include file="includes/footer.jsp" %>
