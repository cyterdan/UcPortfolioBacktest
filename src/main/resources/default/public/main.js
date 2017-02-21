var data = [
[],[],[],
[],[]
];
$(document).ready(function(){


    var data = [
    [],[],[],
    [],[]
    ];


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


    console.log("start");
    
    
    var container = document.getElementById("grid");
    var hot = new Handsontable(container, {
      data: data,
      rowHeaders: true,
      colHeaders: ['Fonds','%'],
      afterChange : function(){ 
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
          {type: 'numeric',width: 100,format: '0.00 %',}
            ]
    });

    data[0][0] = "FR0010362863";
    data[0][1] = 0.3;
    hot.render();
    

    
    function doGraph(history,reference){
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
                text: 'Performance (sans unité)'
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
            type : 'line',
            name : 'reference',
            data : reference
        }]
    });
    }
    
    
    $('#goButton').button(
                ).click(function() {
        var send=[];
        //console.log(data);
        for(var i=0;i<data.length;i++){
                   send.push({'uc':data[i][0],'part':data[i][1]});
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
            message : "backtest en cours.."
        
        });
        $.ajax({
            url: "/backtest",
            method:"post",
            data  :  {data:send,size:send.length},
            context: document.body,
            
            success: function (data, textStatus, jqXHR) {
                        
                        if(data.status === "ERROR") {
                            alert(data.message);
                        }
                        
                        
                        if(data.status === "OK"){
                            h = []
                            for(var i=0;i<data.history.length;i++){
                                e = []
                                e[0] = Date.UTC(data.history[i][0],data.history[i][1],data.history[i][2]);
                                e[1] = data.history[i][3];
                                h[i] = e;
                            }
                            r = []
                            for(var j=0;j<data.reference.length;j++){
                                e = []
                                e[0] = Date.UTC(data.reference[j][0],data.reference[j][1],data.reference[j][2]);
                                e[1] = data.reference[j][3];
                                r[j] = e;
                            }
                            
                            doGraph(h,r);
                            $('#performance').val(data.perf);
                            $('#volatilite').val(data.std);
                        }
                        
                                
                    },
                 error : function(){
                                   $.unblockUI();

                     alert("Oops ! quelque chose est cassé");
                 }
                 
          }).done(function() {
              $.unblockUI();
          });

  
  });
  });