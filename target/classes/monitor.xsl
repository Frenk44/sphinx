 <?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
    <xsl:template match="header">
        <p>header</p>
        <p>name:<u><xsl:value-of select="name"/></u></p>
        <p>id:<u><xsl:value-of select="id"/></u></p>
    </xsl:template>
-->
    <xsl:template match="payload">
            <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="*[not(*)]">
        <tr><td><xsl:value-of select="local-name()"/></td><td><xsl:value-of select="."/></td></tr>
    </xsl:template>
    <xsl:template match="*[(*)]">
        <b>***<xsl:value-of select="local-name()"/></b> <br/>
        <table>
            <xsl:apply-templates />
        </table>
    </xsl:template>

     <xsl:template match="/">
         <html>
             <head>
                 <title>monitor</title>
                 <style>
                     H1 {font-family: Arial,Univers,sans-serif;font-size: 36pt; }
                     table, th, td {
                     border: 1px solid black;
                     border-collapse: collapse;
                     }
                 </style>
             </head>
             <body>
                 <script type="text/javascript" language="javascript">
                 var http_request = false;
                 var ser = Math.round(Math.random()*1000000); // Anti-caching serial number
                 var debug = false; // Set to true to show the full server response

                 function debug(text)
                 {
                     alert(text);
                    }


                 function ajax(httpRequestMethod, url, parameters, target)
                 {
                     alert('ajax');
                 http_request = false;
                 document.getElementById(target).innerHTML = 'Wait...'
                 if (window.XMLHttpRequest)
                 { // For Mozilla, Safari, Opera, IE7
                     http_request = new XMLHttpRequest();
                     if (http_request.overrideMimeType)
                     {
                         http_request.overrideMimeType('text/plain');
                         //Change MimeType to match the data type of the server response.
                         //Examples: "text/xml", "text/html", "text/plain"
                     }
                 }
                 else if (window.ActiveXObject)
                 { // For IE6
                 try
                 {
                 http_request = new ActiveXObject("Msxml2.XMLHTTP");
                 }
                 catch (e)
                 {
                 try
                 {
                 http_request = new ActiveXObject("Microsoft.XMLHTTP");
                 }
                 catch (e)
                 {}
                 }
                 }
                 if (!http_request)
                 {
                 alert('Giving up :( Cannot create an XMLHTTP instance');
                 return false;
                 }
                 http_request.onreadystatechange = function() {updateElement(target);};
                 if (httpRequestMethod == 'GET')
                 {
                 http_request.open('GET', url + '?' + parameters, true);
                 http_request.send(null);
                 ser = ser + 1;
                 }
                 else if (httpRequestMethod == 'POST')
                 {
                 http_request.open('POST', url, true);
                 http_request.setRequestHeader('Content-Type',
                 'application/x-www-form-urlencoded');
                 http_request.send(parameters);
                 }
                 else
                 {
                 alert('Sorry, unsupported HTTP method');
                 }
                 }

                 function updateElement(target)
                 {
                 if (http_request.readyState == 4)
                 {
                 if (debug == true)
                 {
                 alert(http_request.responseText);
                 }
                 if (http_request.status == 200)
                 {
                 document.getElementById(target).innerHTML =
                 http_request.responseText;
                 }
                 else if (debug == false)
                 {
                 alert('The server returned an error. Please set debug = true to see the full server response.');
                 }
                 }
                 }
                    </script>
                 <span style="cursor: pointer; color: #0000FF;">UPDATE</span>

                 <div id="dataBox" style="width:380px; border:solid black 1px; padding:2px;">
                     Div element. Content will be replaced by data from the server.
                 </div>
                 <span style="cursor: pointer; text-decoration: underline; color: #0000FF;"
                       onclick="ajax2('GET','http://localhost:8001/sphinx','id=1','dataBox')">

                     AJAX UPDATE
                 </span>
                 <span onclick="debug('hello world')">
                     TEST ONCLICK
                 </span>
                  <xsl:apply-templates/>
             </body>
         </html>
     </xsl:template>

</xsl:stylesheet>