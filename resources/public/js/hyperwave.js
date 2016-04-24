function printer (data) {
    console.log(data);
    data = JSON.parse(data);
    console.log(data);
}

function postHtml(post) {
    return "<div class=\"post row\" id=\""+post.id+"\"><div class=\"post-body five columns\">"+post.body+"</div><div class=\"author two columns\">"+post.author+"</div><div class=\"one column\"></div></div>";
};

function replaceAll(str, find, replace) {
  return str.replace(new RegExp(find, 'g'), replace);
}

function postId(post) {
    return "#"+replaceAll(post.id, /:/, "\\:");
}

function chainUpdateStar(elem, id) {
    $.get("/api/v0/p/"+id, function(data) {
        data = JSON.parse(data);
        if(data.status == "OK") {
            console.log(data);
            var post = data.body.ar;
            var next = data.body.dr;
            var newId = postId(post);
            if($(newId).length == 0) {
                $(elem).insertAfter(postHtml(post));
                if(next != null) {
                    chainUpdateStar(newId, next);
                }
            }
        }
    });
}

function chainUpdate(id) {
    $.get("/api/v0/p/"+id, function(data) {
        data = JSON.parse(data);
        if(data.status == "OK") {
            var next = data.body.dr;
            if(next != null) {
                chainUpdateStar(id, next);
            }
        }
    });
}

$("#send").submit(function(event) {
    event.preventDefault();
    var $form = $(this),
        url = $form.attr("action");
    $.post(url, $form.serialize(), function(data) {
        data = JSON.parse(data);
        if(data.status == "OK") {
            var post = data.body;
            var id = postId(post);
            if($(id).length == 0) {
                $("#feed").prepend(postHtml(post));
                changed = post.id;
            }
            chainUpdate(id);
        }
    }).fail(printer);
    $form[0].reset();
});

function updateFeed(lastTimeout) {
    $.get("/api/v0/p", function(data) {
        data = JSON.parse(data);
        posts = data.body;
        posts.reverse();

        var changed = false;
        for(p in posts) {
            post = posts[p];
            var id = postId(post);
            if($(id).length == 0) {
                $("#feed").prepend(postHtml(post));
                changed = post.id;
            }
        }

        if(changed != false && lastTimeout == 0) {
            lastTimeout = 1000;                
        } else if (changed != false) {
            console.log("Found new posts! Setting timeout to 1000, chain updating!");
            lastTimeout = 1000;
            chainUpdate(changed);
        } else if (lastTimeout < 5000) {
            lastTimeout = lastTimeout * 2;
            console.log("No new posts, slowing refresh to " + lastTimeout);
        }
        setTimeout(function () {updateFeed(lastTimeout);}, lastTimeout);
    });
}

updateFeed(0);
