#version 430 core

#define AMBIENT_LIGHT 0.1
#define REFLECTIVITY 0.1

#define LARGE_FLOAT 1E+10

#define MAX_SPHERES 1000
#define MAX_LIGHTS 100

#define REFLECTIONS 2

#define Kd 0.9
#define Ks 0.1

#define EPSILON 0.0001

layout(binding = 0, rgba32f) uniform image2D framebufferImage;

//View Frustum
uniform vec3 eye, ray00, ray01, ray10, ray11;


struct Light {
    vec3 position;
    vec3 colour;
    float brightness;
};

struct Material {
    vec3 albedo;
    float specularN;
};

struct Sphere {
    vec3 position;
    float radius;
    Material mat;
};

struct Intersection {
    float lambda; //Lambda x DirectionVector + Origin = Point of intersection
    vec3 surfaceNormal;
    int  sphereIndex;
};

struct Ray {
    vec3 origin;
    vec3 direction;
    Intersection intersect;
    bool reflects;
};

uniform int numberOfLights;
uniform int numberOfSpheres;
uniform Light sceneLights[MAX_LIGHTS];
uniform Sphere sceneSpheres[MAX_SPHERES];

float intersectSphere(vec3 origin, vec3 dir, const Sphere s) {
    float a = dot(dir, dir);
    vec3 fromSphere = origin - s.position;

    float b = 2.0 * dot(dir, fromSphere);
    float c = dot(fromSphere, fromSphere) - (s.radius * s.radius);

    float dscrmnt = b * b - 4.0 * a * c;
    if(dscrmnt < 0.0) {
        //No intersection
        return -1.0;
    }

    float solution = (-b - sqrt(dscrmnt)) / (2.0 * a);

    return solution;
}

//Return true if the given Ray intersects with an entity in the scene
//And store the intersection information in ray's `intersection' member
bool intersect(inout Ray toIntersect) {
    //Used to find the closest intersection (smallest lambda) set to large number to ensure no clipping / lost entities at large lambdas
    float smallest = LARGE_FLOAT;

    //If an intersection was found this is true
    bool found = false;

    //Normalize direction vector
    toIntersect.direction = normalize(toIntersect.direction);

    //For all spheres in scene
    for (int i = 0; i < numberOfSpheres; i++) {
        Sphere sphere = sceneSpheres[i];

        //Calculate lambda of intersection with the current sphere
        float lambda = intersectSphere(toIntersect.origin, toIntersect.direction, sphere);

        //If Lambda is in correct direction and is smaller than current smallest Lambda (Closer to origin)
        if(lambda > EPSILON && lambda < smallest) {
            //Update smallest lambda
            smallest = lambda;
            //Found an intersection
            found = true;

            //Set intersection information
            toIntersect.intersect.lambda = lambda;
            //Surface normal at point of intersection
            toIntersect.intersect.surfaceNormal = normalize(toIntersect.origin + (toIntersect.direction * lambda) - sphere.position);
            //Index of sphere we intersected with
            toIntersect.intersect.sphereIndex = i;
        }
    }
    //No intersections, nullify intersection member
    if(!found) {
        toIntersect.intersect.lambda = 0;
        toIntersect.intersect.surfaceNormal = vec3(0);
        toIntersect.intersect.sphereIndex = -1;
    }

    return found;
}


vec3 trace(inout Ray toTrace) {
    bool intersection = intersect(toTrace);

    //Color of this point
    vec3 colour = vec3(AMBIENT_LIGHT);
    toTrace.reflects = intersection;

    if(!intersection) {
        return colour;
    }


    //Point of intersection
    vec3 P = toTrace.origin + (toTrace.intersect.lambda * toTrace.direction);

    //Trace to light
    for (int i = 0; i < numberOfLights; i++) {
        Light light = sceneLights[i];

        //vec3 toLightRay = (light.position - P);
        //CHECK THIS
        vec3 lightDirection = -normalize(light.position - P);

        //CHECK THIS
        Ray toLightRay = Ray(P - toTrace.intersect.surfaceNormal, lightDirection, Intersection(0.0f, vec3(0), -1), false);
        Sphere sphere = sceneSpheres[toTrace.intersect.sphereIndex];

        //If light can reach the point unobstructed
        if (!intersect(toLightRay)) {

            //Distance and R^2
            float dist = distance(P, light.position);
            float r2 = dist * dist;

            //Diffuse
            vec3 diffuseColour = (vec3(light.colour) * (light.brightness / r2));

            //Specular
            vec3 R = reflect(lightDirection, toLightRay.intersect.surfaceNormal);

            //CHECK THIS
            vec3 specularColour = pow(max(0.0f, dot(R, -toTrace.direction)), sphere.mat.specularN) * light.brightness / r2 * light.colour;

            //Final colour
            colour += (Kd * diffuseColour) + (Ks * specularColour);
        } else {
            //CHECK THIS
            //colour += AMBIENT_LIGHT * sphere.mat.albedo;
            //return vec3(1.0, 0.0, 0.0);
        }
    }

    return colour;
}

layout (local_size_x = 16, local_size_y = 16) in;

/**
 * Entry point of this GLSL compute shader.
 */
void main(void) {
    /*
     * Obtain the 2D index of the current compute shader work item via
     * the built-in gl_GlobalInvocationID variable and store it in a 'px'
     * variable because we need it later.
     */
    ivec2 px = ivec2(gl_GlobalInvocationID.xy);
    /*
     * Also obtain the size of the framebuffer image. We could have used
     * a custom uniform for that as well. But GLSL already provides it as
     * a built-in function.
     */
    ivec2 size = imageSize(framebufferImage);
    /*
     * Because we have to execute our compute shader with a global work
     * size that is a power of two, we need to check whether the current
     * work item is still within our actual framebuffer dimension so that
     * we do not accidentally write to or read from unallocated memory
     * later.
     */
    if (any(greaterThanEqual(px, size)))
    return; // <- no work to do, return.
    /*
     * Now we take our rayNN uniforms declared above to determine the
     * world-space direction from the eye position through the current
     * work item's pixel's center in the framebuffer image. We use the
     * 'px' variable, cast it to a floating-point vector, offset it by
     * half a pixel's width (in whole pixel units) and then transform that
     * position relative to our framebuffer size to get values in the
     * interval [(0, 0), (1, 1)] for all work items covering our
     * framebuffer.
     */
    vec2 p = (vec2(px) + vec2(0.5)) / vec2(size);
    /*
     * Use bilinear interpolation based on the X and Y fraction
     * (within 0..1) with our rayNN vectors defining the world-space
     * vectors along the corner edges of the camera's view frustum. The
     * result is the world-space direction of the ray originating from the
     * camera/eye center through the work item's framebuffer pixel center.
     */
    vec3 dir = mix(mix(ray00, ray01, p.y), mix(ray10, ray11, p.y), p.x);
    /*
     * Now, trace the list of boxes with the ray `eye + t * dir`.
     * The result is a computed color which we will write at the work
     * item's framebuffer pixel.
     */
    Ray primeRay = Ray(eye, normalize(dir), Intersection(0, vec3(0), -1), false);

    //Trace prime ray
    vec3 color = trace(primeRay);

    //do reflection traces
    Ray reflectionBounces[REFLECTIONS];
    vec3 colours[REFLECTIONS];
    reflectionBounces[0] = primeRay;
    colours[0] = color;
    for(int i = 1; i < REFLECTIONS; i++) {
        //Calculate point of intersection as origin
        Ray currentRay = reflectionBounces[i - 1];
        vec3 reflectionOrigin = currentRay.origin + (currentRay.direction * currentRay.intersect.lambda);

        //Calculate reflection direction
        vec3 reflectionDirection = reflect(currentRay.direction, currentRay.intersect.surfaceNormal);

        //Trace new ray
        Ray reflectedRay = Ray(reflectionOrigin, reflectionDirection , Intersection(0, vec3(0), -1), false);
        vec3 reflectedColour = trace(reflectedRay);

        colours[i] = reflectedColour;

        //Store in reflection bounces
        reflectionBounces[i] = reflectedRay;
    }

    float exponent = pow(REFLECTIVITY, REFLECTIONS);
    for(int i = 0; i < REFLECTIONS; i++) {
        color += colours[REFLECTIONS - i] * exponent;
        exponent /= REFLECTIVITY;
    }
    /*
     * Store the final color in the framebuffer's pixel of the current
     * work item.
     */
    imageStore(framebufferImage, px, vec4(color, 1.0));
}