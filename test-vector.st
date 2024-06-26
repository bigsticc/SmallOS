class Vector3 implementing Arithmetic is
    var x.
    var y.
    var z.
    
    static def x: anX y: aY z: aZ as
        var result := self new.
        result x: anX.
        result y: aY.
        result z: aZ.
    end
    
    @getSet: x // pragmas can invoke compiler scripts and generate code; this one generates an "x" and "x:" method.
    @getSet: y
    @getSet: z

    def length as
        ^(x squ + y squ + z squ) sqrt. 
    end
    
    @override
    def + other as
        var result := Vector3 new.
        result x: x + other x.
        result y: y + other y.
        result z: z + other z.
        ^result.
    end
    
    @override
    def - other as
        var result := Vector3 new.
        result x: x - other x.
        result y: y - other y.
        result z: z - other z.
        ^result.
    end
    
    @override
    def * scalar as
        var result := Vector3 new.
        result x: x * scalar.
        result y: y * scalar.
        result z: z * scalar.
        ^result.
    end
    
    @override
    def / scalar as
        var result := Vector3 new.
        result x: x / scalar.
        result y: y / scalar.
        result z: z / scalar.
        ^result.
    end
    
    def dot: other as
        ^(x * other x) + (y * other y) + (z * other z).
    end
end

trait Arithmetic is
    require + other.
    require - other.
    require * other.
    require / other.
end