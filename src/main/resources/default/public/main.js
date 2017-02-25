var data = [
    [], [], [],
    [], [], [],
    [], [], [],
    [], [], [],
    [], [], []
];
var hot = {};
$(document).ready(function () {



    function customDropdownRenderer(instance, td, row, col, prop, value, cellProperties) {
        var selectedId;
        var optionsList = cellProperties.chosenOptions.data;
        var values = (value + "").split(",");
        var value = [];
        for (var index = 0; index < optionsList.length; index++) {
            if (values.indexOf(optionsList[index].id + "") > -1) {
                selectedId = optionsList[index].id;
                value.push(optionsList[index].label);
            }
        }
        value = value.join(", ");
        Handsontable.TextCell.renderer.apply(this, arguments);
    }



    var TextEditor = Handsontable.editors.TextEditor;
    var NumericFormattedEditor = TextEditor.prototype.extend();
    NumericFormattedEditor.prototype.beginEditing = function (initialValue) {
        var format = this.cellProperties.format;
        var formattedValue = numeral(this.originalValue).format(format);
        TextEditor.prototype.beginEditing.apply(this, [formattedValue]);
        this.TEXTAREA.select();
    };
    var container = document.getElementById("grid");
    hot = new Handsontable(container, {
        data: data,
        rowHeaders: true,
        colHeaders: ['Fonds', '%'],
        beforeChange: function (changes, source) {
            for (var i = 0; i < changes.length; i++) {
                var cellChange = changes[i];
                var col = cellChange[1];
                var newCellValue = cellChange[3];
                if (col === 1) {

                    var hasExhibitionSuffix = new RegExp("%$").test(newCellValue);
                    if (!hasExhibitionSuffix) {
                        newCellValue += "%";
                    }
                cellChange[3] = numeral().unformat(newCellValue);
                }

            }
        },
        afterChange: function () {
            $('#perf').html("");
            $('#std').html("");
            $('#history').html("");
            $('#permalink').html("");
        },
        columns: [
            {
                renderer: customDropdownRenderer,
                editor: "chosen",
                width: 500,
                chosenOptions: {
                    data: funds
                }
            },
            {type: 'numeric', width: 100, format: '0.00%', editor: NumericFormattedEditor}
        ]
    });
    //data[0][0] = "FR0010362863";
    //data[0][1] = 1.0;
    hot.render();
    function doGraph(history, reference, perf, std) {
        Highcharts.chart('history', {
            chart: {
                zoomType: 'x'
            },
            title: {
                text: 'Historique de ce portefeuille'
            },
            subtitle: {
                text: document.ontouchstart === undefined ?
                        'Click and drag in the plot area to zoom in' : 'Pinch the chart to zoom in'
            },
            xAxis: {
                type: 'datetime'


            },
            yAxis: {
                title: {
                    text: 'Performance (base 1)'
                }
            },
            legend: {
                enabled: true
            },
            plotOptions: {
                area: {
                    fillColor: {
                        linearGradient: {
                            x1: 0,
                            y1: 0,
                            x2: 0,
                            y2: 1
                        },
                        stops: [
                            [0, Highcharts.getOptions().colors[0]],
                            [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                        ]
                    },
                    marker: {
                        radius: 2
                    },
                    lineWidth: 1,
                    states: {
                        hover: {
                            lineWidth: 1
                        }
                    },
                    threshold: null
                }
            },
            series: [{
                    type: 'area',
                    name: 'Backtest',
                    data: history
                },
                {
                    type: 'line',
                    name: 'reference',
                    data: reference
                }]
        });
        Highcharts.chart('perf', {
            chart: {
                type: 'gauge',
                plotBackgroundColor: null,
                plotBackgroundImage: null,
                plotBorderWidth: 0,
                plotShadow: false
            },
            title: {
                text: 'Performance annuelle'
            },
            pane: {
                startAngle: -120,
                endAngle: 120,
                background: [{
                        backgroundColor: {
                            linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                            stops: [
                                [0, '#FFF'],
                                [1, '#333']
                            ]
                        },
                        borderWidth: 0,
                        outerRadius: '109%'
                    }, {
                        backgroundColor: {
                            linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                            stops: [
                                [0, '#333'],
                                [1, '#FFF']
                            ]
                        },
                        borderWidth: 1,
                        outerRadius: '107%'
                    }, {
                        // default background
                    }, {
                        backgroundColor: '#DDD',
                        borderWidth: 0,
                        outerRadius: '105%',
                        innerRadius: '103%'
                    }]
            },
            // the value axis
            yAxis: {
                min: 0.0,
                max: 35.0,
                minorTickInterval: 'auto',
                minorTickWidth: 1,
                minorTickLength: 10,
                minorTickPosition: 'inside',
                minorTickColor: '#666',
                tickPixelInterval: 30,
                tickWidth: 2,
                tickPosition: 'inside',
                tickLength: 10,
                tickColor: '#666',
                labels: {
                    step: 1,
                    rotation: 0,
                    overflow: 'none',
                    endOnTick: false,
                    formatter: function () {
                        if (this.value == 1.4)
                            return 'Inflation';
                        if (this.value == 2.0)
                            return 'Livret A';
                        if (this.value == 4.0)
                            return 'Obligation';
                        if (this.value == 5.0)
                            return 'Or';
                        if (this.value == 6)
                            return 'SCPI';
                        if (this.value == 9)
                            return 'Logements';
                        if (this.value == 10)
                            return 'Actions';
                        if (this.value == 14)
                            return 'Foncières';
                        return '';
                    },
                    style: {
                        color: 'black',
                        fontSize: '15px'
                    }
                },
                title: {
                    text: 'Performance annuelle <br/> moyenne(%)'
                },
                plotBands: [{
                        from: 0.0,
                        to: 2.0,
                        color: '#DF5353' // red
                    }, {
                        from: 2.0,
                        to: 7.0,
                        color: '#DDDF0D' // yellow
                    }, {
                        from: 7.0,
                        to: 120.0,
                        color: '#55BF3B' // green
                    }]
            },
            series: [{
                    name: 'Performance hebdomadaire',
                    data: [parseFloat(perf.toFixed(2))],
                    tooltip: {
                        valueSuffix: '%'
                    },
                    dataLabels: {
                        enabled: true,
                        style: {
                            fontSize: '40px'
                            
                        }
                    }
                }

            ]

        });
        Highcharts.chart('std', {
            chart: {
                type: 'gauge',
                plotBackgroundColor: null,
                plotBackgroundImage: null,
                plotBorderWidth: 0,
                plotShadow: false
            },
            title: {
                text: 'Volatilité hebdomadaire'
            },
            pane: {
                startAngle: -120,
                endAngle: 120,
                background: [{
                        backgroundColor: {
                            linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                            stops: [
                                [0, '#FFF'],
                                [1, '#333']
                            ]
                        },
                        borderWidth: 0,
                        outerRadius: '109%'
                    }, {
                        backgroundColor: {
                            linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
                            stops: [
                                [0, '#333'],
                                [1, '#FFF']
                            ]
                        },
                        borderWidth: 1,
                        outerRadius: '107%'
                    }, {
                        // default background
                    }, {
                        backgroundColor: '#DDD',
                        borderWidth: 0,
                        outerRadius: '105%',
                        innerRadius: '103%'
                    }]
            },
            // the value axis
            yAxis: {
                min: 0.0,
                max: 40.0,
                minorTickInterval: 'auto',
                minorTickWidth: 1,
                minorTickLength: 10,
                minorTickPosition: 'inside',
                minorTickColor: '#666',
                tickPixelInterval: 30,
                tickWidth: 2,
                tickPosition: 'inside',
                tickLength: 10,
                tickColor: '#666',
                labels: {
                    step: 1,
                    rotation: 0,
                    overflow: 'none',
                    endOnTick: false,
                  
                    
                    style: {
                        color: 'black',
                        fontSize: '15px'
                    }
                },
                title: {
                    text: '%'
                },
                plotBands: [ {
                        from: 0.0,
                        to: 5.0,
                        color: '#55BF3B' // green
                    }, {
                        from: 5.0,
                        to: 10.0,
                        color: '#DDDF0D' // yellow
                    }, {
                        from: 10.0,
                        to: 100.0,
                        color: '#DF5353' // red
                    }]
            },
            series: [{
                    name: 'Volatilité hebdomadaire',
                    data: [parseFloat(std.toFixed(2))],
                    tooltip: {
                        valueSuffix: '%'
                    }, dataLabels: {
                        enabled: true,
                        style: {
                            fontSize: '40px'
                            
                        }
                    }
                }]

        });
    }


    $('#goButton').button(
            ).click(function () {
        var send = [];
        data = hot.getData();
        for (var i = 0; i < data.length; i++) {
            send.push({'uc': data[i][0], 'part': data[i][1]});
        }



        $.blockUI({
            css: {
                border: 'none',
                padding: '5px',
                backgroundColor: '#000',
                '-webkit-border-radius': '10px',
                '-moz-border-radius': '10px',
                opacity: .5,
                color: '#fff'
            },
            message: "backtest en cours.."

        });
        
        
        $.ajax({
            url: "/backtest",
            method: "post",
            data: {data: send, size: send.length,rebalanceMode : $("#rebalance").val(),benchmark : $("#reference").val()},
            context: document.body,
            success: function (data, textStatus, jqXHR) {

                if (data.status === "ERROR") {
                    alert(data.message);
                }


                if (data.status === "OK") {
                    h = []
                    for (var i = 0; i < data.history.length; i++) {
                        e = []
                        e[0] = Date.UTC(data.history[i][0], data.history[i][1]-1, data.history[i][2]);
                        e[1] = data.history[i][3];
                        h[i] = e;
                    }
                    
                    r = []
                    for (var j = 0; j < data.reference.length; j++) {
                        e = []
                        e[0] = Date.UTC(data.reference[j][0], data.reference[j][1]-1, data.reference[j][2]);
                        e[1] = data.reference[j][3];
                        r[j] = e;
                    }

                    //console.log(h);
                    doGraph(h, r, data.perf, data.std);
                    $("#permalink").html("<a href='" + data.permalink + "' > lien permanent vers ce portefeuil </a>");
                }


            },
            error: function () {
                $.unblockUI();
                alert("Oops ! quelque chose est cassé");
            }

        }).done(function () {
            $.unblockUI();
        });
    });
});