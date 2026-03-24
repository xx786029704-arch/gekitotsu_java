package org.example.elements.atk;

import org.example.Main;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.Core;
import org.example.elements.hit.HitsDrop;

public class ConMissileBullet extends Bullet {   //梱玉导弹分弹
    private float speed = 12F;
    private int cnt = 0;
    private final int targetId;
    private float desiredRot;
    private float rot;

    public ConMissileBullet(float X, float Y, int S, int rotation, int targetId) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.desiredRot = rotation;
        this.targetId = targetId;
        this.r *= 0.6F;
        updateVelocity();
    }

    @Override
    public void step() {   //每帧逻辑
        cnt++;
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2 || cnt > 200) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
            kill();
            return;
        }
        this.rot = normalizeRotation(this.rot);
        float targetRot = computeTargetRot();
        if (this.rot - 180 > targetRot) {
            targetRot += 360;
            desiredRot = targetRot;
        }
        if (this.rot + 180 < targetRot) {
            targetRot -= 360;
            desiredRot = targetRot;
        }
        if (this.speed < 15F) {
            this.speed += 0.3F;
        }
        if (this.speed > 14F) {
            this.rot = this.rot + (targetRot - this.rot) / 4F;
            this.rot = normalizeRotation(this.rot);
        }
        updateVelocity();
        move();
    }

    private float computeTargetRot() {   //计算追踪角度
        if (targetId != -1) {
            if (Main.elements.containsKey(targetId)) {
                Shape target = Main.elements.get(targetId);
                desiredRot = Math.round((float) (Math.atan2(target.y - this.y, target.x - this.x) * 180 / Math.PI));
                return desiredRot;
            }else{
                return desiredRot;
            }
        }
        desiredRot = Math.round((float) (Math.atan2(Main.core_y[1 - this.side] - this.y, Main.core_x[1 - this.side] - this.x) * 180 / Math.PI));
        return desiredRot;
    }

    private void updateVelocity() {   //更新速度向量
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
    }

    private float normalizeRotation(float rotation) {   //角度归一化
        float normalized = rotation % 360F;
        if (normalized > 180F) {
            normalized -= 360F;
        } else if (normalized < -180F) {
            normalized += 360F;
        }
        return normalized;
    }
}
