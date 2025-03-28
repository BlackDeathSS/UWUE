fade(false, function () 
    setMusicPitch(1) 
    objVar("player", "pos", {0, 100, 150}) 
    objVar("player", "rotY", 0)    
    fade(true, function () 
        showDialog("You cant leave this place...") 
    end) 
end)

grassyFall = true
