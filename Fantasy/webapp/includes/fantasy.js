function debug(msg) {
    try {
        java.lang.System.out.println(msg);
    } catch (e) {
        var err=e;
        var txt=msg;
    }
}

// copy a value from one element to another.
// Useful for when a select box can populate
// a text input box.
function copyValue(from, to) {
    var a = document.getElementById(from);
    var b = document.getElementById(to);
    b.value = a.value;
}

function goURL(url, args) {
    var newLoc = jsprops.get(url);
    if (args != null) {
       newLoc += '?';
       newLoc += args;
    }
    debug("going to " + newLoc);
    window.location = newLoc;
}

function moveRank(from, to) {
	indx=1;
	if (from>to)
		indx=-1;
	for (i=from; i!=to; i+=indx) {
        // swap the player names
		var a=document.getElementById(i);
		var b=document.getElementById(i+indx);
		var t=a.innerHTML;
		a.innerHTML=b.innerHTML;
		b.innerHTML=t;

        // swap the player ids
        var a=document.getElementById("id" + i);
        var b=document.getElementById("id" + (i+indx));
        var t=a.value;
        a.value=b.value;
        b.value=t;
	}
}

function fetchFranchisePositions(id) {
    var args = "franchiseId=" + document.getElementById(id).value;
    goURL('franchise.positions.url', args);
}

function setLeaguePositionCount(name, position) {
	var num = this.form.name.options[this.form.name.selectedIndex].value;
    var args = "position=" + position + "&num=" + num;
    goURL('league.position.count.set.url', args);
}

function increment(id, amt) {
    var x = document.getElementById(id);
    var v = parseFloat(x.value);
    v += amt;
    x.value = v;
}

function showMessage(text) {
    var elem = document.getElementById("messages");
    elem.style.visibility="";
    elem.innerHtml = text;
}

function setUserActive(id, activate) {
	var args="userId=" + id + "&activate=" + activate;
	goURL('user.setactive.url', args);
}