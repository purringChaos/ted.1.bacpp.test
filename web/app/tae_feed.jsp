<%@ page import="java.util.*, java.text.*, tv.blackarrow.cpp.model.*" language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<script type="text/javascript">
	function isInteger(intStr) {
		return (intStr.search(/^[0-9]+$/) == 0 )
	}
	
	function resetForm() {
		var frm = document.trigger_ap_form;
		frm.reset();
	}
	function triggerEvent() {
		var frm = document.trigger_ap_form;
		var hasErrors = false;
		var message = "Please correct the following errors and try again";
		if(frm.feedId.value.length == 0) {
			hasErrors = true;
			message = message + "\nEnter Feed Id"
		}
		if(frm.eventTime.value.length == 0) {
			hasErrors = true;
			message = message + "\nEnter Event Time"
		}

		if(hasErrors) {
			alert(message);
		} else {
			var execFrm = document.exec_event;
			execFrm.action = "<%= HostSettingBean.getInstance().getTranscoderHost() %>" + ":6601/cpp/feed/" + frm.feedId.value + "/" + frm.eventTime.value;
			execFrm.submit();
		}
	}
</script>
<p><strong> Acquisition Event Feed</strong></p>
<form name=trigger_ap_form method="post" onsubmit="return false;">
	<table cellpadding="3" cellspacing="1" border="0">
		<tr>
			<td width=100>Acquisition Point</td>
			<td width=10>:</td>
			<td><input type=text name=feedId size=20 value="15025"> <i>(Ex 15025)</i></td>
		</tr><tr>
			<td>Event Time</td>
			<td>:</td>
			<td><input type=text name=eventTime size=20 value="2012-04-27 00:25:00"> <i>(Ex: 2012-04-27 00:25:00)</i></td>
		</tr><tr>
			<td colspan=3>
				<input type="button" name="btn_submit" value="Submit" onClick="triggerEvent()">
				<input type="button" name="btn_reset" value="Reset" onClick="resetForm()">&nbsp;
			</td>
		</tr>
	</table>
</form>
<form name="exec_event" method="get"></form>
