package dev.laarryy.Icicle.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("icicle_profiles")
@IdName("userid")
public class User extends Model {
}
