// ==UserScript==
// @id             ingress-portalalert
// @name           PortalAlert
// @version        9
// @namespace      https://github.com/Lorenzbi/portalalert
// @downloadURL    http://portalalert.lorenzz.ch:3000/iitc-portalalert.user.js
// @updateURL      http://portalalert.lorenzz.ch:3000/iitc-portalalert.user.js
// @description    PortalAlert tool for Ingress
// @include        http://www.ingress.com/intel*
// @include        https://www.ingress.com/intel*
// @match          http://www.ingress.com/intel*
// @match          https://www.ingress.com/intel*
// ==/UserScript==

function wrapper() {
    if(typeof window.plugin !== 'function') window.plugin = function() {};
    window.plugin.portalalert = function() {};
    window.plugin.portalalert.portal = [];
    window.plugin.portalalert.icon = L.Icon.Default.extend({options: {
    //iconUrl: 'http://portalalert.lorenzz.ch:3000/images/marker-icon.png',
        //shadowUrl : '',
        iconSize: [32,41],
        iconAnchor: [16,40]
    }});
    window.plugin.portalalert.setup_link = function(data){
        var d = data.portalDetails;
        var guid = data.guid;
        var lat = d.locationE6.latE6 / 1e6
        var lng = d.locationE6.lngE6 / 1e6
        window.plugin.portalalert.portal = {guid: guid, imagesrc: d.imageByUrl.imageUrl, title: d.descriptiveText.map.TITLE, address: d.descriptiveText.map.ADDRESS, lng: lng, lat: lat};
        $('#portaldetails').append('<div class="portalalert"> <a onclick="window.plugin.portalalert.open_dialog()" title="submit portal">Portalalert Submit</a><a onclick="window.plugin.portalalert.upload_visible()" title="Upload all visible portals">Alle Hochladen</a></div>');
    }
    window.plugin.portalalert.submit_portal = function(){
                $.ajax({url: 'http://portalalert.lorenzz.ch:3000/alert',type: 'POST', data:{'portal': JSON.stringify(window.plugin.portalalert.portal)},dataType: 'jsop',success: function(res){console.log(res);}});
    }
    window.plugin.portalalert.sync = function() {
        L.Icon.Default.imagePath = 'http://portalalert.lorenzz.ch:3000/images';
        $.post( "http://portalalert.lorenzz.ch:3000/sync", { lat: map.getCenter().lat.toString(), lng: map.getCenter().lng.toString() }, function( data ) {
            var obj = $.parseJSON(data);
            $.each(obj.alerts, function() {
                var lnglat = this.location.coordinates;
                 var icon = new window.plugin.portalalert.icon();
                L.marker([lnglat[1],lnglat[0]], {icon: icon}).addTo(map);
            });
        });
       
    }
    
    var lastSyncLat;
    var lastSyncLng;
    window.plugin.portalalert.checkDistanceMoved = function() {
        var center = map.getCenter();
        if (!lastSyncLat){
            lastSyncLat = map.getCenter().lat;
            lastSyncLng = map.getCenter().lng;
        }
        var sync = center.distanceTo(L.latLng(lastSyncLat,lastSyncLng));
        console.log(sync);
        if (sync > 3000) {
            console.log('sync');
            window.plugin.portalalert.sync(); 
            lastSyncLat = map.getCenter().lat;
            lastSyncLng = map.getCenter().lng;
        }
    }
    window.plugin.portalalert.open_dialog = function() {
        var dialogtext = "<select id=alert-type><option value=1>Upgrade</option><option value=2>Destroy</option></select><br><select id=alert-ttl><option value=1>1 Stunde</option><option value=3>3 Stunden</option><option value=6>6 Stunden</option><option value=12>12 Stunden</option><option value=24>24 Stunden</option><option value=0>Immer</option></select><br><label>Message</label><textarea id=alert-message></textarea>";
        dialog({
        text: dialogtext,
        title: 'Portal Alert: '+ window.plugin.portalalert.portal.title,
        id: 'portalalert',
        width: 350,
        buttons: {
            'Submit Alert': function() {
                window.plugin.portalalert.portal.type = parseInt($('#alert-type').val());
                window.plugin.portalalert.portal.ttl = parseInt($('#alert-ttl').val()) * 3600000;
                window.plugin.portalalert.portal.message = $('#alert-message').val();
                window.plugin.portalalert.submit_portal();
                $(".ui-dialog").hide("slow");
            }
    }
  });
    }
    window.plugin.portalalert.upload_visible = function(){
        var list = [];
        $.each(window.portals, function(i, portal)
        {
            var d = portal.options.data;
            var thisPortal = {
                '_id': i, // the primary key for couchdb is _id and for that we use the guid
                'title': d.title,
                'image': d.image,
                'location':{'type':'Point','coordinates': [ d.lngE6 / 1e6, d.latE6 / 1e6 ]}
                };
            list.push (thisPortal);
        });
        $.post( "http://portalalert.lorenzz.ch:3000/upload", { portals: list }, function( response ) {
            alert("Portals added to Database. Thank You.")
        }).fail(function(){
            alert("Something went wrong. Please try again later.")
        });
    }
    var setup = function(){
                //map.on('moveend', function() { window.plugin.portalalert.checkDistanceMoved() });

        window.addHook('portalDetailsUpdated', window.plugin.portalalert.setup_link);
        $('head').append('<style>' +
                         '#dialog-portalalert label { display: block; }' +
                         '#dialog-portalalert textarea { background: rgba(0, 0, 0, 0.3); color: #ffce00; height: 120px; width: 100%; padding: 4px 4px 0px 4px; font-size: 12px; border: 0; box-sizing: border-box; }' +
                         '#dialog-portalalert select { background: rgba(0, 0, 0, 0.3); color: #ffce00; border: 0; padding: 5px;}' +
                         '#dialog-portalalert option { background: rgba(8, 48, 78, 0.9); }' +
                         '.ui-dialog button {padding: 5px; }'+
                         '</style>');
    }

    if(window.iitcLoaded && typeof setup === 'function') {setup();} 
    else {
        if(window.bootPlugins) {window.bootPlugins.push(setup);} 
        else {window.bootPlugins = [setup];}
    }
}

var script = document.createElement('script');
script.appendChild(document.createTextNode('('+ wrapper +')();'));
(document.body || document.head || document.documentElement).appendChild(script);