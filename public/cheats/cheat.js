var cheatWindowRows;
var cheatWindowRowsV;
var cheat_window;

function cheat_init() {
	cheat_window = window.open("", "Memory Cheater", "width=1100, height=570");
	cheat_init_window();
}

function cheat_init_window() {
	var doc = cheat_window.document;

	cheatWindowRows = [];
	cheatWindowRowsV = [];
	for (var x = 0; x < 4; x++) {
		cheatWindowRows[x] = [];
		cheatWindowRowsV[x] = [];
		var row = doc.createElement("div");
    var server = window.location.origin;
		for (var y = 0; y < 8; y++) {
			var img = doc.createElement('img');
			img.src = server + '/assets/images/animals/X.jpg';
      console.log(img.src);
			row.appendChild(img);
			cheatWindowRows[x][y] = img;
			cheatWindowRowsV[x][y] = false;
		}
		doc.body.appendChild(row);
	}
	setInterval(cheat_checkImages, 500);
}


function cheat_checkImages(ev) {
	var cards = document.getElementsByTagName('memory-card');

	for (var i = 0; i < cards.length; i++) {
		var id = cards[i].id;

		var re = /x(\d)y(\d)/i;
		var match;
		if ((match = re.exec(id)) !== null) {
			var x = match[1];
			var y = match[2];

			var div = getFirstChildOf(cards[i], 'div');
			div = getFirstChildOf(div, 'div');
			var iron = getFirstChildOf(div, 'iron-image');
			var img = getFirstChildOf(iron, 'img');

			if (!cheatWindowRowsV[x][y] && img.src.substr(-5).toLowerCase() !== 'x.jpg') {
				cheatWindowRows[x][y].src = img.src;
				cheatWindowRowsV[x][y] = true;
			}
		}
	}
}

function getFirstChildOf(node, tag) {
	for (var i = 0; i < node.children.length; i++) {
		var n = node.children[i];
		if (n.tagName.toLowerCase() === tag.toLowerCase()) {
			return n;
		}
	}
	return null;
}

(function() {
	var button = document.createElement("button");
	button.innerHTML = "Cheater";

	button.addEventListener("click", function() {
		cheat_init();
	});

	document.body.appendChild(button);
})();