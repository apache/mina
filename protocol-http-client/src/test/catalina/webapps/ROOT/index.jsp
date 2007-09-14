<%
    Cookie cookie = new Cookie("test","value");
    cookie.setMaxAge(3600);
    response.addCookie(cookie);
%>
Hello World!