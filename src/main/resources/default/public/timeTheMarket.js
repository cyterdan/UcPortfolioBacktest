$(document).ready(function () {

    var startDate;
    var currentDate;
    var lastDate;
    var currentValue;
    var lastEuroPart;
    var hc;

function getVisualForValue(euroPart){
    if(euroPart==1.0) return {color:'#edffd0',text:'0% en actions'};
    if(euroPart==0.8) return {color:'#d3ffba',text:'20% en actions'};
    if(euroPart==0.5) return {color:'#b7ffa2',text:'50%  en actions'};
    if(euroPart==0.3) return {color:'#97ff88',text:'70% en actions'};
    if(euroPart==0.0) return {color:'#70ff69',text:'100% en actions '};;

}

var capital =$("#capital").html();
var msci = $("#msci").html();
var firstChange = true;
var  series;

function dateFromStr(str){
    var parts = str.split("-");
    return Date.UTC(parts[0], parts[1] - 1, parts[2])
}

function addBand(){
             hc.xAxis[0].addPlotBand({
                color: getVisualForValue(lastEuroPart).color, 
                from: lastDate, 
                to: currentDate,
                label: { 
                    text : getVisualForValue(lastEuroPart).text
                }

            });
}

function encode(arg){
    console.log(md5(arg));
    return arg;

}
function end() {
    var html = "Sur une période de 8 ans ("+new Date(startDate).toLocaleDateString()+"-"+new Date(currentDate).toLocaleDateString()+")<br />";
    var perfIndex = Math.round(100*(msci-10000)/10000);
    var perfCap = Math.round(100*(capital-10000)/10000);
    if(capital==msci){
        html += "Vous avez une performance identique à l'index MSCI";
    }
    if(capital>msci){
        surperf =perfCap-perfIndex;
        html += "Vous avez battu l'index de "+surperf+" % !";
    }
    if(capital<msci){
        surperf = perfIndex-perfCap
        html += "Vous avez été battu par l'index qui fait "+surperf+" % de mieux que vous";
    }
    $("#result").html(html);
    $("#reloadLink").show();
    
 
    
}
$('.profileBtn').on('change',function(){
    
    
    if(firstChange){
                console.log("firstChange");
                  //magic number qui correspond à aujourdhui - 10ans...
                    var randomStart = randomIntFromInterval(0, 473);
                    lastDate = dateFromStr(msciWorld[randomStart][0]);
                    
                    startDate = dateFromStr(msciWorld[randomStart][0]);
                    currentDate = lastDate;
                    console.log("lastdate:"+lastDate);
                    var i = randomStart;
                    console.log("start at"+msciWorld[randomStart][0]);
                    
                    var intervalId = setInterval(function () {
                        //console.log(i);
                        if (i < randomStart + 97) {
                            
                            var date = dateFromStr(msciWorld[i][0]),
                                    y = msciWorld[i][1];
                            series.addPoint([date, y]);
                            currentDate = date;
                            currentValue = y;
                            var previous = msciWorld[i-1][1];
                            var perfMsci = (y - previous)/previous;
                            var perfEuro = 0.0003;
                            capital = capital*(1+(perfEuro*lastEuroPart+perfMsci*(1-lastEuroPart)))                                    
                            msci = msci*(1+perfMsci);
                            $("#msci").html(Math.round(msci));
                            $("#capital").html(Math.round(capital));
                            i++;
                        }
                        else {
                            clearInterval(intervalId);
                            console.log("done at" + msciWorld[i][0]);
                            addBand();
                            end();
                        }
                    }, 500);
            firstChange = false

    }
    else{
        console.log("adding band for "+lastEuroPart+" from "+lastDate+" to "+currentDate);
        addBand();
   
    }
    var value = $('input[name=options]:checked').attr('partEnEuro')
    lastEuroPart = value
    lastDate = currentDate;

    
   
});

    Highcharts.setOptions({
        global: {
            useUTC: false
        }
    });

    function randomIntFromInterval(min, max)
    {
        return Math.floor(Math.random() * (max - min + 1) + min);
    }

    hc = Highcharts.chart('container', {
        chart: {
            type: 'line',
            animation: false, // don't animate in old IE
            marginRight: 10,
            events: {
                load: function () {

                    // set up the updating of the chart each second
                     series = this.series[0];
          
                }
            }
        },
        title: {
            text: 'MSCI World'
        },
        xAxis: {
            type: 'datetime', labels: {
                enabled: false
            }


        },
        yAxis: {
            title: {
                text: 'Value'
            },
            plotLines: [{
                    value: 0,
                    width: 1,
                    color: '#808080'
                }]
        },
        tooltip: {
            enabled: false

        },
        legend: {
            enabled: false
        },
        exporting: {
            enabled: true
        },
        series: [{
                name: 'MSCI World',
                data: (function () {
                    // generate an array of random data
                    var data = [],
                            time,
                            i;
                    /*              for (i = -10; i <= 0; i += 1) {
                     data.push({
                     x: msciWorld[0][0],
                     y: 100
                     });
                     }*/


                    return data;
                }())
            }]
    });


});