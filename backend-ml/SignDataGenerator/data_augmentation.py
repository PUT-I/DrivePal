import random

import numpy as np
from PIL import Image, ImageEnhance, ImageFilter


class DataAugmentation:
    """ Constructor
    :param wsize - (0, ∞> width of the image that will be created
    :param obj_size - <0, 1> width of the pasted object on the image. If wsize is 500 px, and obj_size is 0.25,
    the width of the object will be 500 * 0.25 = 125 px
    :param scale_variation - <0, ∞> variation of the base obj_size. It will generate pseudo-random value between 0
    and scale_varation, and multiple object size by it
    :param rotate_variation - <0, 90) variation of the object rotation. Object will be rotated by random value between
    rotate_variation and rotate_variation in y axis
    :param clip_ratio - <0, 1> how much of the object area will be clipped out of the frame
    :param noise_frequency - <0, 1> how much area of the object will be replaced by noise
    :param blur - (False/True) if the blur should be added to object
    :param contrast_factor - <0, ∞> how much contrast between -contrast_factor and contrast_factor
    should be added to image
    :param brightness_factor - <0, ∞> how much brigthness between -brightness_factor and brightness_factor
    should be added to image
    :param saturation_factor - <0, ∞> how much saturation between -saturation_factor and saturation_factor
    should be added to image
    :param random_erasing_factor - <0, 1> how much of object area between 0 and random_erasing_factor
    should be replaced by one black rectangle
    """

    def __init__(self, wsize, obj_size, scale_variation=None, rotate_variation=None, clip_ratio=None,
                 noise_frequency=None, blur=None, contrast_factor=None, brightness_factor=None, saturation_factor=None,
                 random_erasing_factor=None):
        self.wsize = wsize
        self.obj_size = obj_size
        self.scale_variation = scale_variation or 0.0
        self.rotate_variation = rotate_variation or 0.0
        self.clip_ratio = clip_ratio or 0.0
        self.noise_frequency = noise_frequency or 0.0
        self.blur = blur or False
        self.contrast_factor = contrast_factor or 0.0
        self.brightness_factor = brightness_factor or 0.0
        self.saturation_factor = saturation_factor or 0.0
        self.random_erasing_factor = random_erasing_factor or 0.0

    ## PRIVATE FUNCTIONS
    @staticmethod
    def __find_coeffs(source_coords, target_coords):
        matrix = []
        for s, t in zip(source_coords, target_coords):
            matrix.append([t[0], t[1], 1, 0, 0, 0, -s[0] * t[0], -s[0] * t[1]])
            matrix.append([0, 0, 0, t[0], t[1], 1, -s[1] * t[0], -s[1] * t[1]])
        A = np.matrix(matrix, dtype=np.float)
        B = np.array(source_coords).reshape(8)
        res = np.dot(np.linalg.inv(A.T * A) * A.T, B)
        return np.array(res).reshape(8)

    @staticmethod
    def __count_rotation_cords(width, height, angle):
        alfa = 90.0 - angle
        alfa_rad = np.deg2rad(alfa)
        sin_alfa = np.sin(alfa_rad)
        cos_alfa = np.cos(alfa_rad)

        r = width / 2
        a = int(sin_alfa * r)
        b = int(cos_alfa * r)

        if b > 0:
            return [a * 2, height + b * 2], [(0, b), (a * 2, 0), (a * 2, height + b * 2), (0, height + b)]

        return [a * 2, height - b * 2], [(0, 0), (a * 2, -b), (a * 2, height - b), (0, height - b * 2)]

    @staticmethod
    def __noise_generator(image, noise_frequency=0.05):
        for x in range(0, image.size[0]):
            for y in range(0, image.size[1]):
                if random.random() < noise_frequency:
                    gray_color = int(random.random() * 255)
                    image.putpixel((x, y), (gray_color, gray_color, gray_color))
        return image

    ## Image generating function
    def generate_image(self, obj, background):
        # scale background to set width (base 416)
        wpercent = (self.wsize / float(background.size[0]))
        hsize = int((float(background.size[1]) * float(wpercent)))
        background = background.resize((self.wsize, hsize), Image.ANTIALIAS)

        pos_x = 0
        pos_y = 0

        # scale object
        if self.scale_variation != 0.0:
            obj_wsize = int(self.wsize * self.obj_size)
            scale = 1.0 + random.random() * self.scale_variation
            obj_wsize = int(obj_wsize * scale)
            wpercent = (obj_wsize / float(obj.size[0]))
            obj_hsize = int((float(obj.size[1]) * float(wpercent)))
            obj = obj.resize((obj_wsize, obj_hsize), Image.ANTIALIAS)

        # rotate object
        if self.rotate_variation != 0.0:
            rotation_angle = random.random() * self.rotate_variation * 2 - self.rotate_variation
            new_size, target_cords = self.__count_rotation_cords(obj.size[0], obj.size[1], rotation_angle)
            coeffs = self.__find_coeffs(
                [(0, 0), (obj.size[0], 0), (obj.size[0], obj.size[1]), (0, obj.size[1])],
                target_cords)
            obj = obj.transform((new_size[0], new_size[1]), Image.PERSPECTIVE, coeffs,
                                Image.BICUBIC)

        # clipping object (if not translating)
        if self.clip_ratio != 0.0:
            clip_y = random.random() > 0.5

            # clip on top or bottom of the picture
            if clip_y:
                clip_top = random.random() > 0.5
                pos_x = int(random.random() * (self.wsize - obj.size[0]))
                if clip_top:
                    pos_y = int(-self.clip_ratio * obj.size[1])
                else:
                    pos_y = int(hsize - obj.size[1] + (self.clip_ratio * obj.size[1]))
            # clip on the left or right size of the picture
            else:
                clip_left = random.random() > 0.5
                pos_y = int(random.random() * (hsize - obj.size[1]))
                if clip_left:
                    pos_x = int(-self.clip_ratio * obj.size[0])
                else:
                    pos_x = int(self.wsize - obj.size[0] + (self.clip_ratio * obj.size[0]))
        # translat object (if not clipping)
        else:
            pos_x = int(random.random() * (self.wsize - obj.size[0]))
            pos_y = int(random.random() * (hsize - obj.size[1]))

        if self.noise_frequency != 0.0:
            obj = self.__noise_generator(obj, noise_frequency=self.noise_frequency)

        if self.blur:
            if obj.size[0] > 60:
                obj = obj.filter(ImageFilter.BLUR)

        if self.contrast_factor != 0.0:
            contrast_enhancer = ImageEnhance.Contrast(obj)
            contrast_value = 1.0 + (-self.contrast_factor + random.random() * (self.contrast_factor * 2))
            obj = contrast_enhancer.enhance(contrast_value)

        if self.brightness_factor != 0.0:
            brightness_enhancer = ImageEnhance.Brightness(obj)
            brightness_value = 1.0 + (-self.brightness_factor + random.random() * (self.brightness_factor * 2))
            obj = brightness_enhancer.enhance(brightness_value)

        if self.saturation_factor != 0.0:
            saturation_enhancer = ImageEnhance.Color(obj)
            saturation_value = 1.0 + (-self.saturation_factor + random.random() * (self.saturation_factor * 2))
            obj = saturation_enhancer.enhance(saturation_value)

        if self.random_erasing_factor != 0.0:
            erasing_width = int(self.random_erasing_factor * obj.size[0])
            erasing_height = int(self.random_erasing_factor * obj.size[1])
            obj_pos_x = int(random.random() * (obj.size[0] - erasing_width))
            obj_pos_y = int(random.random() * (obj.size[1] - erasing_height))

            for x in range(obj_pos_x, obj_pos_x + erasing_width):
                for y in range(obj_pos_y, obj_pos_y + erasing_height):
                    obj.putpixel((x, y), (0, 0, 0))

        background.paste(obj, (pos_x, pos_y), obj)

        # yolo annotations
        obj_center_x = pos_x + obj.size[0] / 2
        obj_center_y = pos_y + obj.size[1] / 2

        percentage_x = obj_center_x / self.wsize
        percentage_y = obj_center_y / hsize
        percentage_width = obj.size[0] / self.wsize
        percentage_height = obj.size[1] / hsize

        return background, [percentage_x, percentage_y, percentage_width, percentage_height]
