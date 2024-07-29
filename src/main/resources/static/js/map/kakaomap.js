var mapContainer = document.getElementById('map'), // 지도를 표시할 div
    mapOption = {
        center: new kakao.maps.LatLng(37.56587, 126.97668), // 기본 중심 좌표
        level: 3, // 지도의 확대 레벨
        mapTypeId: kakao.maps.MapTypeId.ROADMAP // 지도 종류
    };

// 지도 생성
var map = new kakao.maps.Map(mapContainer, mapOption);

// 지도 타입 변경 컨트롤 생성
var mapTypeControl = new kakao.maps.MapTypeControl();
map.addControl(mapTypeControl, kakao.maps.ControlPosition.TOPRIGHT);

// 확대 축소 컨트롤 생성
var zoomControl = new kakao.maps.ZoomControl();
map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);

// 사용자 현재 위치 가져와서 지도 중심으로 설정
if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(position) {
        var lat = position.coords.latitude, // 위도
            lon = position.coords.longitude; // 경도
        var locPosition = new kakao.maps.LatLng(lat, lon), // 좌표 객체 생성
            message = '<div style="padding:5px;">Current Location</div>'; // 정보 창 내용

        map.setCenter(locPosition); // 지도의 중심을 현재 위치로 이동

        var marker = new kakao.maps.Marker({
            map: map,
            position: locPosition
        });

        // 정보창을 생성하고 지도에 표시합니다
        var infowindow = new kakao.maps.InfoWindow({
            content: message,
            removable: true
        });
        infowindow.open(map, marker);

        // 마커에 클릭 이벤트 등록
        kakao.maps.event.addListener(marker, 'click', function() {
            infowindow.open(map, marker);
        });
    });
} else {
    alert("Unable to use your current location.");
}

// 추가 마커 생성 및 표시
var markerPosition  = new kakao.maps.LatLng(37.56682, 126.97865);
var marker2 = new kakao.maps.Marker({
    position: markerPosition,
    map: map
});

// 도로 경로 설정
var roadPath = [
    new kakao.maps.LatLng(37.56635, 126.98006),
    new kakao.maps.LatLng(37.56682, 126.97865)
];

var content = '<img id="roadImage" src="/img/DJI_0049.JPG" alt="도로 이미지" style="position: absolute;">';

var customOverlay = new kakao.maps.CustomOverlay({
    map: map,
    clickable: true,
    content: content,
    position: roadPath[0],
    xAnchor: 0.5,
    yAnchor: 0.5
});

function adjustOverlaySize() {
    var image = document.getElementById('roadImage');
    if (!image) return;

    var projection = map.getProjection();
    var point1 = projection.pointFromCoords(roadPath[0]);
    var point2 = projection.pointFromCoords(roadPath[1]);
    var width = Math.abs(point2.x - point1.x);
    var height = Math.abs(point2.y - point1.y);

    image.style.width = width + 'px';
    image.style.height = height + 'px';
    customOverlay.setPosition(roadPath[0]);
}

kakao.maps.event.addListener(map, 'zoom_changed', adjustOverlaySize);
kakao.maps.event.addListener(map, 'center_changed', adjustOverlaySize);

adjustOverlaySize();

kakao.maps.event.addListener(marker2, 'click', function() {
    alert('You clicked the additional marker!');
});
