function doGet(e) {
  // Untuk menangani permintaan dari website (CORS support lewat JSONP atau JSON response biasa)
  var action = e.parameter.action;
  
  try {
    // 1. Fitur Geocode: Mengubah Lat Lng menjadi Alamat Lengkap
    if (action === 'geocode') {
      var lat = parseFloat(e.parameter.lat);
      var lng = parseFloat(e.parameter.lng);
      
      // Memanggil layanan internal Google Maps tanpa API Key eksternal
      var response = Maps.newGeocoder().reverseGeocode(lat, lng);
      
      var address = "Alamat tidak ditemukan";
      if (response.status === 'OK' && response.results.length > 0) {
        address = response.results[0].formatted_address;
      }
      
      return ContentService.createTextOutput(JSON.stringify({
        status: 'success',
        address: address
      })).setMimeType(ContentService.MimeType.JSON);
    } 
    
    // Fitur Search: Mengubah kata kunci pencarian menjadi Lat Lng dan Alamat
    else if (action === 'search') {
      var query = e.parameter.q;
      var response = Maps.newGeocoder().geocode(query);
      
      var results = [];
      if (response.status === 'OK' && response.results.length > 0) {
        for (var i = 0; i < response.results.length; i++) {
          results.push({
            name: response.results[i].formatted_address,
            center: {
              lat: response.results[i].geometry.location.lat,
              lng: response.results[i].geometry.location.lng
            },
            bbox: {
              _southWest: {
                lat: response.results[i].geometry.viewport.southwest.lat,
                lng: response.results[i].geometry.viewport.southwest.lng
              },
              _northEast: {
                lat: response.results[i].geometry.viewport.northeast.lat,
                lng: response.results[i].geometry.viewport.northeast.lng
              }
            }
          });
        }
      }
      
      return ContentService.createTextOutput(JSON.stringify({
        status: 'success',
        results: results
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // 2. Fitur Static Map: Mengambil gambar Peta Google Satelit
    else if (action === 'staticmap') {
      var lat = parseFloat(e.parameter.lat);
      var lng = parseFloat(e.parameter.lng);
      var width = parseInt(e.parameter.w) || 280;
      var height = parseInt(e.parameter.h) || 320;
      
      // Membuat gambar peta statis
      var map = Maps.newStaticMap()
        .setSize(width, height)
        .setCenter(lat, lng)
        .setZoom(15)
        .addMarker(lat, lng)
        .setMapType(Maps.StaticMap.Type.ROADMAP)
        .setFormat(Maps.StaticMap.Format.PNG);
        
      var blob = map.getBlob();
      
      // Mengubah gambar menjadi Base64 agar bisa digambar di Canvas website tanpa error CORS
      var base64 = Utilities.base64Encode(blob.getBytes());
      var dataUri = "data:image/png;base64," + base64;
      
      return ContentService.createTextOutput(JSON.stringify({
        status: 'success',
        image: dataUri
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error', 
      message: 'Parameter action tidak valid'
    })).setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error', 
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
