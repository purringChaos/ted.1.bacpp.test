		<p><strong>Alternate Program</strong></p>
		Please fill in Blackout related alternate program XML format data<br /><br /> 
		<form name="uploadPoForm" action="<%= HostSettingBean.getInstance().getTranscoderHost() %>:6630/blackout" method="post" enctype="text/plain">
			<textarea name="blackout" rows="20" cols="120" autofocus="autofocus"></textarea>
			<br /><br />
			<input type="submit" value="Submit" />
			<input type="reset" value="Reset" />
		</form>
