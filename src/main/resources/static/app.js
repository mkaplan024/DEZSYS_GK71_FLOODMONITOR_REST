var format = "json";
var apiKey = "floodmonitor-demo-key";

$(document).ready(function () {
    loadStations();

    $("#station").change(function () {
        loadData();
    });

    $("#filter").click(function () {
        loadData();
    });

    $("input[name=format]").change(function () {
        format = $(this).val();
        loadStations();
    });

    // alle 10 Sekunden neu laden
    setInterval(function () {
        if ($("#autoRefresh").is(":checked")) {
            loadData();
        }
    }, 10000);
});

// GET-Anfrage mit Accept-Header je nach Format und API-Key
function getData(url, callback) {
    $("#loading").show();

    $.ajax({
        url: url,
        dataType: format,
        headers: {
            "Accept": "application/" + format,
            "X-API-KEY": apiKey
        },
        success: callback,
        error: showError,
        complete: function () {
            $("#loading").hide();
        }
    });
}

function loadStations() {
    var selected = $("#station").val();

    getData("/api/v1/stations", function (data) {
        var stations = [];

        if (format == "xml") {
            $(data).find("item").each(function () {
                stations.push({
                    id: $(this).children("id").text(),
                    name: $(this).children("name").text()
                });
            });
        } else {
            stations = data;
        }

        $("#station").empty();
        for (var i = 0; i < stations.length; i++) {
            $("#station").append("<option value='" + stations[i].id + "'>" + stations[i].name + "</option>");
        }

        if (selected) {
            $("#station").val(selected);
        }

        loadData();
    });
}

function loadData() {
    var id = $("#station").val();
    $("#error").text("");

    loadLatest(id);
    loadHistory(id);
}

function loadLatest(id) {
    getData("/api/v1/stations/" + id + "/measurements/latest", function (data) {
        // bei 204 kommt nichts zurück
        if (!data) {
            $("#latest").html("Keine Messung vorhanden");
            return;
        }

        var m;
        if (format == "xml") {
            m = readMeasurement(data);
        } else {
            m = data;
        }

        $("#latest").html(
            "Zeitpunkt: " + formatTime(m.timestamp) + "<br>" +
            "Wasserstand: " + m.waterLevel + " cm<br>" +
            "Durchfluss: " + m.flowRate + " m³/s<br>" +
            "Niederschlag: " + m.rainfall + " mm/h<br>" +
            "Temperatur: " + m.temperature + " °C<br>" +
            "Akku: " + m.batteryLevel + " %<br>" +
            "Status: " + m.status + "<br>" +
            "Warnstufe: <span class='" + m.warningLevel + "'>" + m.warningLevel + "</span>"
        );
    });
}

function loadHistory(id) {
    var url = "/api/v1/stations/" + id + "/measurements?limit=" + $("#limit").val();

    if ($("#from").val() != "") {
        url += "&from=" + new Date($("#from").val()).toISOString();
    }
    if ($("#to").val() != "") {
        url += "&to=" + new Date($("#to").val()).toISOString();
    }
    if ($("#warningLevel").val() != "") {
        url += "&warningLevel=" + $("#warningLevel").val();
    }

    getData(url, function (data) {
        var list = [];

        if (format == "xml") {
            $(data).find("item").each(function () {
                list.push(readMeasurement(this));
            });
        } else {
            list = data;
        }

        $("#measurements").empty();

        if (list.length == 0) {
            $("#measurements").append("<tr><td colspan='8'>Keine Messwerte gefunden</td></tr>");
            return;
        }

        for (var i = 0; i < list.length; i++) {
            var m = list[i];
            $("#measurements").append(
                "<tr>" +
                "<td>" + m.stationId + "</td>" +
                "<td>" + formatTime(m.timestamp) + "</td>" +
                "<td>" + m.waterLevel + "</td>" +
                "<td>" + m.flowRate + "</td>" +
                "<td>" + m.rainfall + "</td>" +
                "<td>" + m.batteryLevel + "</td>" +
                "<td>" + m.status + "</td>" +
                "<td class='" + m.warningLevel + "'>" + m.warningLevel + "</td>" +
                "</tr>"
            );
        }
    });
}

// liest eine Messung aus dem XML aus
function readMeasurement(xml) {
    return {
        stationId: $(xml).find("stationId").text(),
        timestamp: $(xml).find("timestamp").text(),
        waterLevel: $(xml).find("waterLevel").text(),
        flowRate: $(xml).find("flowRate").text(),
        rainfall: $(xml).find("rainfall").text(),
        temperature: $(xml).find("temperature").text(),
        batteryLevel: $(xml).find("batteryLevel").text(),
        status: $(xml).find("status").text(),
        warningLevel: $(xml).find("warningLevel").text()
    };
}

function formatTime(timestamp) {
    return new Date(timestamp).toLocaleString("de-AT");
}

function showError(xhr) {
    var message = "Fehler " + xhr.status;

    if (xhr.status == 0) {
        message = "Server ist nicht erreichbar";
    } else if (xhr.responseText) {
        // Fehler kann JSON oder XML sein
        try {
            message += ": " + JSON.parse(xhr.responseText).message;
        } catch (e) {
            message += ": " + $($.parseXML(xhr.responseText)).find("message").text();
        }
    }

    $("#error").text(message);
}