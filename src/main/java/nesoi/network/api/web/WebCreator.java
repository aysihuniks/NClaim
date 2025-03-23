package nesoi.network.api.web;

import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.nandayo.DAPI.Util;

import java.io.IOException;

public class WebCreator extends NanoHTTPD {
    public WebCreator() throws IOException {
        super(8080);
        if (!isAlive()) {
            start(SOCKET_READ_TIMEOUT, false);
            Util.log("Config interface started on https://" + Bukkit.getIp() + ":8080/");
        } else {
            stop();
            start(SOCKET_READ_TIMEOUT, true);
            Util.log("Config interface started on https://" + Bukkit.getIp() + ":8080/");
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/api/players")) {
            StringBuilder json = new StringBuilder();
            json.append("{\"players\": [");
            Bukkit.getOnlinePlayers().forEach(player -> {
                json.append("\"").append(player.getDisplayName()).append("\",");
            });
            String response;
            if (json.length() > 13) {
                json.setLength(json.length() - 1);
                json.append("]}");
                response = json.toString();
            } else {
                response = "{\"players\": []}";
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", response);
        }
        else if (uri.equals("/settings")) {
            // HTML sayfasını oluştur
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>")
                    .append("<html lang=\"en\">")
                    .append("<head>")
                    .append("<meta charset=\"UTF-8\">")
                    .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                    .append("<title>NCore Config Panel</title>")
                    .append("<link href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap\" rel=\"stylesheet\">")
                    .append("<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH\" crossorigin=\"anonymous\">")
                    .append("<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css\">")
                    .append("<style>")
                    .append(getCSS())
                    .append("</style>")
                    .append("</head>")
                    .append("<body>");

            // Navbar
            html.append("<div class=\"navbar\">")
                    .append("<div class=\"navbar-left\">")
                    .append("<div class=\"logo\">NCore</div>")
                    .append("<div class=\"navbar-menu\">")
                    .append("<div class=\"dropdown-left\">")
                    .append("<a href=\"#\">Plugins</a>")
                    .append("<div class=\"dropdown-content\">")
                    .append("<a href=\"#\">NClaim</a>")
                    .append("<a href=\"#\" class=\"closed\">NReport</a>")
                    .append("<a href=\"#\" class=\"closed\">NFriend</a>")
                    .append("</div>")
                    .append("</div>")
                    .append("</div>")
                    .append("</div>")
                    .append("<div class=\"dropdown-right\">")
                    .append("<i class=\"bi bi-person-circle profile-icon\"></i>")
                    .append("<div class=\"dropdown-content\">")
                    .append("<a href=\"#\">Settings</a>")
                    .append("<hr style=\"border-color: #404040; margin: 5px 0;\">")
                    .append("<a href=\"#\" class=\"closed\">Logout</a>")
                    .append("</div>")
                    .append("</div>")
                    .append("</div>");

            html.append("<div class=\"container\">")
                    .append("<h2 class=\"text-center mb-4\" style=\"font-weight: 600;\">NClaim - Settings</h2>")
                    .append("<div class=\"card-container\">");

            Setting.getSettings().forEach(setting -> {
                html.append("<div class=\"card\">")
                        .append("<div class=\"card-prefix\">").append(setting.title).append("</div>")
                        .append("<div class=\"card-content\">")
                        .append("<p>").append(setting.description).append("</p>");

                if (setting.type.equals("string")) {
                    html.append("<input type=\"text\" class=\"form-control\" id=\"").append(setting.title.replace(" ", "_")).append("Input\" value=\"").append(setting.value).append("\">");
                } else if (setting.type.equals("boolean")) {
                    html.append("<div class=\"form-check form-switch\">")
                            .append("<input class=\"form-check-input toggle-input\" type=\"checkbox\" id=\"").append(setting.title.replace(" ", "_")).append("\"")
                            .append(((Boolean) setting.value) ? " checked" : "").append(">")
                            .append("<label class=\"form-check-label\" for=\"").append(setting.title.replace(" ", "_")).append("\"></label>")
                            .append("</div>");
                }

                html.append("</div>")
                        .append("</div>");
            });

            html.append("</div>")
                    .append("</div>")
                    .append("<button id=\"saveButton\" class=\"save-btn\">Save</button>")
                    .append("<button id=\"resetButton\" class=\"reset-btn\">Set to Default</button>")
                    .append(getJavaScript())
                    .append("</body>")
                    .append("</html>");

            return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
        }

        return newFixedLengthResponse("404 - Bulunamadı");
    }

    // CSS’yi ayrı bir metodda tutuyoruz
    private String getCSS() {
        return "body { background-color: #0f0f0f; color: #e0e0e0; font-family: 'Poppins', sans-serif; margin: 0; padding: 0; overflow-x: hidden; }" +
                ".navbar { background: linear-gradient(90deg, #1a1a1a, #252525); padding: 20px 40px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.6); display: flex; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 1000; }" +
                ".navbar-left { display: flex; align-items: center; gap: 20px; }" +
                ".navbar .logo { font-size: 1.8rem; font-weight: 600; color: #fff; text-transform: uppercase; transition: all 0.3s ease; cursor: pointer; }" +
                ".navbar .logo:hover { color: #00d4ff; transform: scale(1.05); }" +
                ".navbar-menu a { color: #e0e0e0; text-decoration: none; font-size: 1.1rem; padding: 8px 16px; transition: all 0.3s ease; }" +
                ".navbar-menu a:hover { color: #00d4ff; }" +
                ".dropdown-left, .dropdown-right { position: relative; }" +
                ".dropdown-content { display: none; position: absolute; background-color: #252525; border-radius: 8px; box-shadow: 0 6px 12px rgba(0, 0, 0, 0.5); min-width: 200px; z-index: 1000; border: 1px solid #00d4ff; opacity: 0; transform: translateY(10px); transition: all 0.3s ease; }" +
                ".dropdown-left .dropdown-content { left: 0; }" +
                ".dropdown-right .dropdown-content { right: 0; }" +
                ".dropdown-content a { color: #e0e0e0; padding: 12px 20px; display: block; text-decoration: none; font-size: 1rem; transition: all 0.3s ease; }" +
                ".dropdown-content a:hover { background-color: #333; color: #00d4ff; }" +
                ".dropdown-left:hover .dropdown-content, .dropdown-right:hover .dropdown-content { display: block; opacity: 1; transform: translateY(0); }" +
                ".dropdown-content a.closed { opacity: 0.5; pointer-events: none; }" +
                ".profile-icon { font-size: 2rem; color: #e0e0e0; transition: all 0.3s ease; }" +
                ".profile-icon:hover { color: #00d4ff; }" +
                ".container { max-width: 1200px; margin: 40px auto; padding: 0 20px; }" +
                ".card-container { display: flex; flex-wrap: wrap; gap: 25px; justify-content: center; }" +
                ".card { background-color: #1c1c1c; border-radius: 12px; box-shadow: 0 6px 15px rgba(0, 0, 0, 0.4); width: 100%; max-width: 280px; transition: transform 0.3s ease; }" +
                ".card:hover { transform: translateY(-5px); }" +
                ".card-prefix { background: linear-gradient(135deg, #00d4ff, #007acc); padding: 12px; color: #fff; font-weight: 600; text-align: center; border-top-left-radius: 12px; border-top-right-radius: 12px; }" +
                ".card-content { padding: 20px; text-align: center; display: flex; flex-direction: column; justify-content: flex-start; min-height: 140px; }" +
                ".card-content p { margin: 0 0 15px; color: #b0b0b0; font-size: 0.95rem; }" +
                ".form-control { background-color: #252525; border: 1px solid #404040; color: #ffffff; border-radius: 6px; padding: 8px; width: 100%; max-width: 200px; margin: 0 auto; }" +
                ".form-control::placeholder { color: #e0e0e0; opacity: 0.8; }" +
                ".form-control:focus { background-color: #252525; border-color: #00d4ff; color: #ffffff; outline: none; box-shadow: 0 0 5px rgba(0, 212, 255, 0.5); }" +
                ".form-check-input { background-color: #252525; border-color: #404040; }" +
                ".form-check-input:hover { cursor: pointer; }" +
                ".form-check-input:checked { background-color: #00d4ff; border-color: #00d4ff; }" +
                ".form-switch { display: flex; justify-content: center; align-items: center; gap: 10px; }" +
                ".save-btn, .reset-btn { position: fixed; bottom: 30px; padding: 12px 30px; border-radius: 10px; font-size: 1.1rem; font-weight: 600; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5); border: none; opacity: 0; transform: scale(0.9); transition: opacity 0.3s ease, transform 0.3s ease; pointer-events: none; }" +
                ".save-btn.show, .reset-btn.show { opacity: 1; transform: scale(1); pointer-events: auto; }" +
                ".save-btn { background: linear-gradient(135deg, #5cff77, #00cc44); color: #fff; right: 30px; }" +
                ".reset-btn { background: linear-gradient(135deg, #ff5c5c, #cc0000); color: #fff; right: 150px; }" +
                ".save-btn:hover, .reset-btn:hover { cursor: pointer; transform: scale(1.05); box-shadow: 0 6px 18px rgba(0, 0, 0, 0.6); }" +
                "@media (max-width: 768px) { .navbar { padding: 15px 20px; } .navbar-left { flex-direction: column; align-items: flex-start; } .card-container { flex-direction: column; align-items: center; } }";
    }

    private String getJavaScript() {
        return "<script>" +
                "document.querySelectorAll('.dropdown-content a').forEach(item => {" +
                "    item.addEventListener('click', function(event) {" +
                "        if (this.classList.contains('closed')) {" +
                "            event.preventDefault();" +
                "        }" +
                "    });" +
                "});" +
                "document.addEventListener(\"DOMContentLoaded\", function () {" +
                "    const saveBtn = document.querySelector(\".save-btn\");" +
                "    const resetBtn = document.querySelector(\".reset-btn\");" +
                "    const inputs = document.querySelectorAll('.form-control, .form-check-input');" +
                "    inputs.forEach(input => {" +
                "        const initialValue = input.type === 'checkbox' ? input.checked : input.value;" +
                "        input.addEventListener('change', function() {" +
                "            const hasChanges = Array.from(inputs).some(i => i.type === 'checkbox' ? i.checked !== initialValue : i.value !== initialValue);" +
                "            if (hasChanges) {" +
                "                saveBtn.classList.add('show');" +
                "                resetBtn.classList.add('show');" +
                "            } else {" +
                "                saveBtn.classList.remove('show');" +
                "                resetBtn.classList.remove('show');" +
                "            }" +
                "        });" +
                "    });" +
                "});" +
                "</script>" +
                "<script src=\"https://cdn.jsdelivr.net/npm/@popperjs/core@2.11.8/dist/umd/popper.min.js\" integrity=\"sha384-I7E8VVD/ismYTF4hNIPjVp/Zjvgyol6VFvRkX/vR+Vc4jQkC+hVqc2pM8ODewa9r\" crossorigin=\"anonymous\"></script>" +
                "<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.min.js\" integrity=\"sha384-0pUGZvbkm6XF6gxjEnlmuGrJXVbNuzT9qBBavbLwCsOGabYfZo0T0to5eqruptLy\" crossorigin=\"anonymous\"></script>";
    }
}